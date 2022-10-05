package core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class Start {

	public static void main(String[] args) {

		AtomicInteger atomicInt = new AtomicInteger(1);

		int numberOfCopies = 5;
		int numberOfDeletes = 5;

		Path directoryForRemoval = Paths.get("C:/xampp/htdocs");
		Path directoryForCopying = Paths.get("C:/Users/mario/Downloads/prestashop_1.7.8.7");
		Path directoryForPasting = directoryForRemoval;

		List<Path> directorysForRemoval = new ArrayList<>();
		List<Path> directorysForCopying = new ArrayList<>();

		int cpu = Runtime.getRuntime().availableProcessors();
		CountDownLatch latch1 = new CountDownLatch(numberOfDeletes);
		CountDownLatch latch2 = new CountDownLatch(numberOfCopies);
		ExecutorService executor = Executors.newFixedThreadPool(cpu);

		for (int i = 1; i <= numberOfDeletes; i++) {
			directorysForRemoval.add(Paths.get("prestashop_COPY" + i));
		}

		directorysForCopying.add(Paths.get("prestashop"));

		for (Path dirToRemove : directorysForRemoval) {
			executor.execute(() -> {
				try {
					Files.walk(directoryForRemoval.resolve(dirToRemove)).sorted(Comparator.reverseOrder())
							.map(Path::toFile).forEach(File::delete);
					latch1.countDown();
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		}

		try {
			latch1.await();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		System.out.println("Done deleting!");

		for (Path dirToCopy : directorysForCopying) {
			for (int j = 0; j < numberOfCopies; j++) {
				executor.execute(() -> {
					try {
						copyDirectoryJavaNIO(directoryForCopying.resolve(dirToCopy), directoryForPasting
								.resolve(dirToCopy.toString() + "_COPY" + atomicInt.getAndIncrement()));
						latch2.countDown();
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
			}
		}
		
		try {
			latch2.await();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.out.println("Done copying!");

		executor.shutdown();

	}

	public static void copyDirectoryJavaNIO(Path source, Path target) throws IOException {

		if (Files.isDirectory(source)) {
			if (Files.notExists(target)) {
				Files.createDirectories(target);
			}

			try (Stream<Path> paths = Files.list(source)) {
				paths.forEach(p -> copyDirectoryJavaNIOWrapper(p, target.resolve(source.relativize(p))));
			}

		} else {
			Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	// extract method to handle exception in lambda
	public static void copyDirectoryJavaNIOWrapper(Path source, Path target) {

		try {
			copyDirectoryJavaNIO(source, target);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
