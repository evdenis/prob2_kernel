package de.prob.web.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.servlet.AsyncContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import de.prob.web.AbstractSession;
import de.prob.web.WebUtils;
import de.prob.web.worksheet.renderer.IRender;
import de.prob.webconsole.ServletContextListener;
import de.prob.worksheet.ScriptEngineProvider;

public class Worksheet extends AbstractSession {

	@Inject
	public Worksheet(ScriptEngineProvider sep) {
		groovy = sep.get();
	}

	private final Logger logger = LoggerFactory.getLogger(Worksheet.class);
	private int boxcount = 0;
	private final Map<String, Box> boxes = Collections
			.synchronizedMap(new HashMap<String, Box>());
	private final List<String> order = Collections
			.synchronizedList(new ArrayList<String>());

	private String defaultboxtype = "Markdown";
	public final ScriptEngine groovy;

	@Override
	public String html(String clientid, Map<String, String[]> parameterMap) {
		ArrayList<Object> scopes = new ArrayList<Object>();
		scopes.add(WebUtils.wrap("clientid", clientid, "default-box-type",
				defaultboxtype));
		scopes.add(WebUtils.wrap("help-markdown",
				WebUtils.render("ui/worksheet/help_markdown.html", null)));
		String render = WebUtils.render("ui/worksheet/index.html",
				scopes.toArray());
		return render;
	}

	public Object reorder(Map<String, String[]> params) {
		String boxId = params.get("box")[0];
		int newpos = Integer.parseInt(params.get("newpos")[0]);
		System.out.println("Reodered box " + boxId + ". New position: "
				+ newpos);
		return null;
	}

	public Object setDefaultType(Map<String, String[]> params) {
		String type = params.get("type")[0];
		defaultboxtype = type;
		return WebUtils.wrap("cmd", "Worksheet.setDefaultType", "type", type);
	}

	public Object switchType(Map<String, String[]> params) {
		String type = params.get("type")[0];
		String id = params.get("box")[0];
		logger.trace("Switch type of {} to {}", id, type);
		IRender typeRenderer = getTypeRenderer(type);
		Box box = new Box(id, type, typeRenderer.getTemplate(),
				typeRenderer.useCodemirror(), typeRenderer.getExtraInfo());
		Box oldBox = boxes.get(id);
		box.setContent(oldBox.getContent());
		boxes.put(id, box);
		return box.replaceMessage();
	}

	public Object deleteBox(Map<String, String[]> params) {
		String box = params.get("number")[0];
		logger.trace("Delete box {}", box);
		int index = order.indexOf(box);
		order.remove(index);
		boxes.remove(box);
		Map<String, String> deleteCmd = WebUtils.wrap("cmd",
				"Worksheet.deleteBox", "id", box);
		if (order.size() > 0)
			return deleteCmd;
		else {
			Box freshbox = appendFreshBox();
			Map<String, String> renderCmd = freshbox.createMessage();
			return new Object[] { deleteCmd, renderCmd };
		}
	}

	public Object leaveEditor(Map<String, String[]> params) {
		String boxId = params.get("box")[0];
		String direction = params.get("direction")[0];
		String text = params.get("text")[0];

		logger.trace("Leaving {} direction {}. Content {}", new Object[] {
				boxId, direction, text });

		List<Object> messages = new ArrayList<Object>();

		if ("down".equals(direction)) {
			messages.addAll(leaveEditorDown(boxId, text));
		}

		if ("up".equals(direction) && !boxId.equals(firstBox())) {
			messages.add(WebUtils.wrap("cmd", "Worksheet.unfocus", "number",
					boxId));
			String focused = getPredecessor(boxId);
			messages.add(WebUtils.wrap("cmd", "Worksheet.focus", "number",
					focused, "direction", "up"));
		}
		boxes.get(boxId).setContent(text);
		messages.addAll(renderBox(boxId, text));
		return messages.toArray(new Object[messages.size()]);
	}

	private List<Object> renderBox(String boxId, String text) {
		ArrayList<Object> res = new ArrayList<Object>();
		Box box = boxes.get(boxId);
		String type = box.type;
		IRender renderer = getTypeRenderer(type);
		if (renderer == null) {
			res.add(WebUtils.wrap("cmd", "Worksheet.render", "box", boxId,
					"html", "No renderer found"));
		} else {
			res.addAll(renderer.render(boxId, text, this));
		}
		return res;
	}

	@SuppressWarnings("unchecked")
	private IRender getTypeRenderer(String type) {
		String className = "de.prob.web.worksheet.renderer." + type;

		Class<IRender> clazz = null;
		IRender renderer = null;
		try {
			clazz = (Class<IRender>) Class.forName(className);
			renderer = ServletContextListener.INJECTOR.getInstance(clazz);
		} catch (Exception e) {
			return null;
		}
		return renderer;
	}

	private List<Object> leaveEditorDown(String boxId, String text) {
		ArrayList<Object> res = new ArrayList<Object>();
		res.add(WebUtils.wrap("cmd", "Worksheet.unfocus", "number", boxId));
		if (boxId.equals(lastBox())) {
			res.add(appendFreshBox().createMessage());
		} else {
			String focused = getSuccessor(boxId);
			res.add(WebUtils.wrap("cmd", "Worksheet.focus", "number", focused,
					"direction", "down"));
		}
		return res;

	}

	private String getSuccessor(String boxId) {
		int index = order.indexOf(boxId) + 1;
		return order.get(index);
	}

	private String getPredecessor(String boxId) {
		int index = order.indexOf(boxId) - 1;
		return order.get(index);
	}

	private String firstBox() {
		return order.isEmpty() ? null : order.get(0);
	}

	private String lastBox() {
		int index = order.size() - 1;
		return index >= 0 ? order.get(index) : null;
	}

	public Box makeBox(String type) {
		IRender typeRenderer = getTypeRenderer(type);
		Box box = new Box(boxcount++, type, typeRenderer.getTemplate(),
				typeRenderer.useCodemirror(), typeRenderer.getExtraInfo());
		box.setContent(typeRenderer.initialContent());
		boxes.put(box.id, box);
		return box;
	}

	@Override
	public void outOfDateCall(String client, int lastinfo, AsyncContext context) {
		super.outOfDateCall(client, lastinfo, context);
		Box box = appendFreshBox();
		Map<String, String> renderCmd = box.createMessage();
		Map<String, String> focusCmd = WebUtils.wrap("cmd", "Worksheet.focus",
				"number", box.id);

		submit(renderCmd, focusCmd);
	}

	private Box appendFreshBox() {
		Box box = makeBox(defaultboxtype);
		order.add(box.id);
		return box;
	}
}
