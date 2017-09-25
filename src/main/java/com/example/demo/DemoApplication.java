package com.example.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;
import org.zalando.problem.Problem;
import org.zalando.problem.ProblemModule;
import org.zalando.problem.spring.web.advice.ProblemHandling;
import org.zalando.problem.spring.web.advice.SpringAdviceTrait;
import org.zalando.problem.validation.ConstraintViolationProblemModule;

import java.util.concurrent.CompletableFuture;

@SpringBootApplication
@EnableAsync
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper()
				.registerModule(new ProblemModule())
				.registerModule(new ConstraintViolationProblemModule());
	}

	@RestController("/")
	static class DummyController {

		private final DummyService service;

		public DummyController(DummyService service) {
			this.service = service;
		}

		@GetMapping("/sync")
		public String sync(@RequestParam(value = "exception", required = false) boolean exception) {
			return service.getFoo(exception);
		}

		@GetMapping("/async")
		public CompletableFuture<String> async(@RequestParam(value = "exception", required = false) boolean exception) {
			return service.getFooAsync(exception);
		}
	}

	@Service
	static class DummyService {
		public String getFoo(boolean exception) {
			if (exception) {
				throw new IllegalArgumentException("blabla");
			}
			return "foo";
		}

		@Async
		public CompletableFuture<String> getFooAsync(boolean exception) {
			if (exception) {
				throw new IllegalArgumentException("blabla");
			}
			return CompletableFuture.completedFuture("foo");
		}
	}

	@ControllerAdvice
	static class DummyControllerAdvice implements ProblemHandling, SpringAdviceTrait {

		@ExceptionHandler(IllegalArgumentException.class)
		public ResponseEntity<?> handleRuntimeException(NativeWebRequest req, IllegalArgumentException e) {
			if (req.getParameter("string") != null) {
				return ResponseEntity.badRequest().body("ERROR");
			} else {
				return create(HttpStatus.BAD_REQUEST, e, req);
			}
		}
	}
}
