package de.prob.check.json;

import de.prob.check.tracereplay.check.TraceCheckerUtils;
import de.prob.check.tracereplay.check.TraceModifier;
import org.apache.groovy.util.Maps;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TraceModifierUtilsTest {


	@Test
	public void zip_test(){
		List<String> first = Arrays.asList("a", "b", "c");
		List<String> second = Arrays.asList("1", "2", "3");

		Map<String, String> result = TraceCheckerUtils.zip(first, second);

		Map<String, String> expected = Maps.of("a", "1", "b", "2", "c", "3");

		Assert.assertEquals(expected, result);
	}


	@Test
	public void zip_empty_test(){
		List<String> first = Collections.emptyList();
		List<String> second = Collections.emptyList();

		Map<String, String> result = TraceCheckerUtils.zip(first, second);

		Map<String, String> expected = Collections.emptyMap();

		Assert.assertEquals(expected, result);
	}

}
