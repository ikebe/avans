package me.geso.avans.jackson;

import me.geso.avans.Controller;
import me.geso.webscrew.response.ByteArrayResponse;
import me.geso.webscrew.response.WebResponse;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public interface JacksonJsonView extends Controller {

	public default WebResponse renderJSON(int statusCode, Object obj) {
		final ObjectMapper mapper = this.createObjectMapper();
		byte[] json;
		try {
			json = mapper.writeValueAsBytes(obj);
		} catch (final JsonProcessingException e) {
			// It caused by programming error.
			throw new RuntimeException(e);
		}

		final ByteArrayResponse res = new ByteArrayResponse();
		res.setStatus(statusCode);
		res.setContentType("application/json; charset=utf-8");
		res.setContentLength(json.length);
		res.setBody(json);
		return res;
	}

	/**
	 * Rendering JSON by jackson.
	 *
	 * @param obj
	 * @return
	 */
	@Override
	public default WebResponse renderJSON(Object obj) {
		return this.renderJSON(200, obj);
	}

	public default ObjectMapper createObjectMapper() {
		final ObjectMapper mapper = new ObjectMapper();
		mapper.configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true);
		return mapper;
	}
}
