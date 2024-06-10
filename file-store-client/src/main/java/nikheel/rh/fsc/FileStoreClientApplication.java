package nikheel.rh.fsc;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import nikheel.rh.fsc.domain.ErrorMessage;
import nikheel.rh.fsc.util.ChecksumCalculator;

@SpringBootApplication
public class FileStoreClientApplication implements CommandLineRunner {
	private final RestTemplate restTemplate;
	private static String BASE_URL;
//	private static Logger LOGGER = LoggerFactory.getLogger(FileStoreClientApplication.class);

	public FileStoreClientApplication() {
		this.restTemplate = new RestTemplate();
	}

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(FileStoreClientApplication.class);
		app.setWebApplicationType(WebApplicationType.NONE);
		app.run(args);
	}

	@Override
	public void run(String... args) throws Exception {
		if (args.length < 2) {
			System.out.println("Usage: java -jar your-app.jar <base-url> <command> [<args>...]");
			return;
		}

		BASE_URL = args[0];
		String command = args[1];

		switch (command) {
		case "add":
			if (args.length < 3) {
				System.out.println("Usage: add file1.txt [file2.txt]");
				return;
			}
			String[] filenames = new String[args.length - 2];
			System.arraycopy(args, 2, filenames, 0, filenames.length);

			callAddService(filenames);
			break;
		case "ls":
			callListService();
			break;
		case "rm":
			if (args.length < 3) {
				System.out.println("Usage: rm file.txt");
				return;
			}
			callRemoveService(args[2]);
			break;
		case "update":
			if (args.length < 3) {
				System.out.println("Usage: update file.txt");
				return;
			}
			callUpdateService(args[2]);
			break;
		case "wc":
			callWordCountService();
			break;
		case "freq-words":
			String limit = null;
			String order = null;
			for (int i = 2; i < args.length; i++) {
				if (args[i].startsWith("--limit=") || args[i].startsWith("-n=")) {
					limit = args[i].split("=")[1];
				} else if (args[i].startsWith("--order=")) {
					order = args[i].split("=")[1];
				}
			}
			callFrequentWordsService(limit, order);
			break;
		default:
			System.out.println("Unknown command: " + command);
		}
	}

	private void callAddService(String... filenames) {
		if (filenames == null || filenames.length == 0) {
			System.out.println("At least one file must be provided.");
			return;
		}
		String url = BASE_URL + "/addFiles";
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		try {
			for (String filename : filenames) {
				body.add("files", loadFile(filename));
			}
		} catch (IOException e) {
			System.out.println("Error loading file: " + e.getMessage());
			return;
		}
		callService(url, HttpMethod.POST, body, new HashMap<>());
	}

	private Resource loadFile(String filename) throws IOException {

		File file = new File(filename);
		if (!file.exists()) {
			throw new IOException("File " + filename + " not found");
		}
		return new FileSystemResource(file);
	}

	private void callListService() {
		String url = BASE_URL + "/list";
		callService(url, HttpMethod.GET, null, new HashMap<>());
	}

	private void callRemoveService(String file) {
		String url = BASE_URL + "/delete?fileName=" + file;
		callService(url, HttpMethod.DELETE, null, new HashMap<>());
	}

	private void callUpdateService(String file) {
		try {
			String checksum = ChecksumCalculator.calculateChecksum(file);
			if (isChecksumAvailableOnServer(checksum)) {
				callAddMetadataService(file, checksum);
			} else {
				String url = BASE_URL + "/update";
				MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
				body.add("file", loadFile(file));
				callService(url, HttpMethod.POST, body, new HashMap<>());
			}
		} catch (IOException | NoSuchAlgorithmException e) {
			System.out.println("Error processing file: " + e.getMessage());
		}
	}

	private boolean isChecksumAvailableOnServer(String checksum) {
		String url = BASE_URL + "/exists/" + checksum;
		try {
			ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
			return response.getStatusCode() == HttpStatus.OK && Boolean.parseBoolean(response.getBody());
		} catch (HttpStatusCodeException ex) {
			handleErrorResponse(ex.getResponseBodyAsString());
			return false;
		}
	}

	private void callAddMetadataService(String file, String checksum) {
		String url = BASE_URL + "/addMetaData";
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		Map<String, String> body = new HashMap<>();
		body.put("fileName", file);
		body.put("checksum", checksum);

		HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

		try {
			ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
			if (response.getStatusCode() == HttpStatus.OK) {
				System.out.println("Metadata added successfully: " + response.getBody());
			} else {
				handleErrorResponse(response.getBody());
			}
		} catch (HttpStatusCodeException ex) {
			handleErrorResponse(ex.getResponseBodyAsString());
		}
	}

	private void callWordCountService() {
		String url = BASE_URL + "/getWordCount";
		callService(url, HttpMethod.GET, null, new HashMap<>());
	}

	private void callFrequentWordsService(String limit, String order) {
		StringBuilder url = new StringBuilder(BASE_URL + "/getFrequentwords");
		if (limit != null || order != null) {
			url.append("?");
			if (limit != null) {
				url.append("limit=").append(limit);
			}
			if (order != null) {
				if (limit != null) {
					url.append("&");
				}
				url.append("order=").append(order);
			}
		}
		callService(url.toString(), HttpMethod.GET, null, new HashMap<>());
	}

	private void callService(String url, HttpMethod method, MultiValueMap<String, Object> body, Map<String, ?> uriVariables) {
		try {
			HttpHeaders headers = new HttpHeaders();
			if (method == HttpMethod.POST || method == HttpMethod.PUT) {
				headers.setContentType(MediaType.MULTIPART_FORM_DATA);
			}

			HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

			ResponseEntity<String> response = restTemplate.exchange(url, method, requestEntity, String.class, uriVariables);

			if (response.getStatusCode() == HttpStatus.OK) {
				System.out.println("Response: " + response.getBody());
			} else {
				handleErrorResponse(response.getBody());
			}
		} catch (HttpStatusCodeException ex) {
			handleErrorResponse(ex.getResponseBodyAsString());
		}
	}

	private void handleErrorResponse(String responseBody) {
		try {
			ErrorMessage errorDetails = restTemplate.getForObject(responseBody, ErrorMessage.class);
			System.out.println("Error occurred:");
			System.out.println("Time: " + errorDetails.getErrorOccurTime());
			System.out.println("Code: " + errorDetails.getErrorCode());
			System.out.println("Message: " + errorDetails.getErrorMessage());
			System.out.println("Details: " + errorDetails.getErrorDetails());
		} catch (Exception e) {
			System.out.println("Error occurred while parsing the error response.");
			System.out.println("Response body: " + responseBody);
		}
	}

}