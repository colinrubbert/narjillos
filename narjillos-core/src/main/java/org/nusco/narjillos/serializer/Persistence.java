package org.nusco.narjillos.serializer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.nusco.narjillos.experiment.Experiment;

public class Persistence {

	private static final String EXPERIMENT_EXT = ".exp";
	private static final String TEMP_EXT = ".tmp";
	private static final String EXPERIMENT_FILE_REGEXP = "\\.exp$";

	public static void save(Experiment experiment) {
		try {
			String tempFileName = experiment.getId() + TEMP_EXT;

			GZIPOutputStream outputStream = new GZIPOutputStream(
					new BufferedOutputStream(
							new FileOutputStream(new File(tempFileName))));
			byte[] dataBytes = JSON.toJson(experiment, Experiment.class).getBytes(Charset.forName("UTF-8"));
			outputStream.write(dataBytes);
			outputStream.close();

			forceMoveFile(tempFileName, experiment.getId() + EXPERIMENT_EXT);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static Experiment loadExperiment(String fileName) {
		String fileNameWithoutExperimentExtension = fileName.split(EXPERIMENT_FILE_REGEXP)[0];
		File tempFile = new File(fileNameWithoutExperimentExtension + TEMP_EXT);

		if (new File(fileName).exists()) {
			Experiment result = deserializeExperimentFrom(fileName);

			// Experiment loaded. Any temp file can be safely deleted.
			if (tempFile.exists())
				tempFile.delete();
			
			return result;
		}
		
		// There is no file with the exact name requested. Try to recover the
		// experiment from a matching temp file, if it exists.
		String experimentFile = fileNameWithoutExperimentExtension + EXPERIMENT_EXT;

		if (tempFile.exists())
			recoverExperimentFile(tempFile, new File(experimentFile));

		// At this point, our only hope is that the user skipped the file
		// extension and only provided the experiment id. Try to load a file
		// with that name, plus the experiment extension. Just fail if such
		// a file doesn't exist.
		return deserializeExperimentFrom(experimentFile);
	}

	public static String loadDNADocument(String fileName) {
		try {
			byte[] encoded = Files.readAllBytes(Paths.get(fileName));
			String dnaDocument = new String(encoded, Charset.defaultCharset());
			return dnaDocument;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String readApplicationVersion() {
		try {
			return Files.readAllLines(Paths.get("version")).get(0);
		} catch (IOException e) {
			return "0.0.0";
		}
	}

	private static void recoverExperimentFile(File tempFile, File experimentFile) {
		try {
			Files.move(Paths.get(tempFile.getName()), Paths.get(experimentFile.getName()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static Experiment deserializeExperimentFrom(String fileName) {
		Experiment experiment;

			checkVersion(fileName);

			String data = readExperimentData(fileName);

			experiment = JSON.fromJson(data, Experiment.class);
			
		experiment.timeStamp();
		return experiment;
	}

	private static String readExperimentData(String fileName) {
		try {
			GZIPInputStream inputStream = new GZIPInputStream(
					new BufferedInputStream(
							new FileInputStream(new File(fileName))));

			// From "Stupid Scanner Tricks", weblogs.java.net/blog/pat/archive/2004/10/stupid_scanner_1.html
			Scanner s = new Scanner(inputStream);
			s.useDelimiter("\\A");
			String data = s.next();
			s.close();
			
			inputStream.close();
			return data;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static void checkVersion(String fileName) {
		String fileNameWithoutExtension = stripExtension(fileName);
		if (!fileNameWithoutExtension.matches("\\d+\\-\\d+\\.\\d+.\\d+")) {
			System.out.println("WARNING: This experiment doesn't contain a version in the filename. " +
					"I cannot check that it was generated by the same version of Narjillos that you're using now.");
			return;
		}
		
		String experimentVersion = extractVersion(fileNameWithoutExtension);
		String applicationVersion = readApplicationVersion();
		if (!experimentVersion.equals(applicationVersion))
			System.out.println("WARNING: This experiment was started with version " + experimentVersion + ", not the current "
					+ applicationVersion + ". The results might be non-deterministic.");
	}

	private static String extractVersion(String fileName) {
		return fileName.substring(fileName.indexOf("-") + 1);
	}

	private static String stripExtension(String fileName) {
		return fileName.substring(0, fileName.lastIndexOf("."));
	}

	private static void forceMoveFile(String source, String destination) throws IOException {
		Path destinationPath = Paths.get(destination);
		if (Files.exists(destinationPath))
			Files.delete(destinationPath);
		Files.move(Paths.get(source), destinationPath);
	}
}