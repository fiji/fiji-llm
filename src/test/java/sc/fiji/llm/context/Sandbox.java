package sc.fiji.llm.context;

import org.junit.Test;

import com.google.gson.JsonObject;

public class Sandbox {
	
	@Test
	public void doAthing() {
		JsonObject err = new JsonObject();
		err.addProperty("error_msg", "hello world");
		System.out.println(err.toString());
	}
}
