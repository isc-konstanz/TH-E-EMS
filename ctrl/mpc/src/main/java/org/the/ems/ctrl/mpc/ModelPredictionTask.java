package org.the.ems.ctrl.mpc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelPredictionTask implements Runnable {
	private final static Logger logger = LoggerFactory.getLogger(ModelPredictionTask.class);

	private final String python;
	private final String script;

	public ModelPredictionTask(String python, String script) {
		this.python = python;
		this.script = script;
	}

	@Override
	public void run() {
		if (logger.isDebugEnabled()) {
			logger.debug("Running TH-E-MPC at {}", LocalTime.now());
		}
		
		String[] command = {
				python,
				script
		};
//		try {
//			Process process = Runtime.getRuntime().exec(command);
//			process.waitFor();
//			if (process.exitValue() != 1) {
//				logger.warn("TH-E-M failed with error: ", readResult(process.getErrorStream()));
//				return;
//			}
//			
//			String result = readResult(process.getInputStream());
//			logger.debug("Read result: ", result);
//			// TODO:
//			
//		} catch (IOException | InterruptedException e) {
//			logger.warn("Error while executing th-e-optimization: {}", e.getMessage());
//		}
	}

	private String readResult(InputStream stdin) throws IOException {
		BufferedReader stdbuff = new BufferedReader(new InputStreamReader(stdin));
		StringBuilder response = new StringBuilder();
		String s = null;
		while ((s = stdbuff.readLine()) != null) {
			response.append(s);
		}
		return response.toString();
	}

}
