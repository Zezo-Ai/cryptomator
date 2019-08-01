package org.cryptomator.ui.fxapp;

import dagger.Module;
import dagger.Provides;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.util.Duration;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.settings.Settings;

import javax.inject.Named;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

@Module
public abstract class UpdateCheckerModule {

	private static final URI LATEST_VERSION_URI = URI.create("https://api.cryptomator.org/updates/latestVersion.json");
	private static final Duration UPDATE_CHECK_INTERVAL = Duration.hours(3);

	@Provides
	@Named("latestVersion")
	@FxApplicationScoped
	static StringProperty provideLatestVersion() {
		return new SimpleStringProperty();
	}

	@Provides
	@FxApplicationScoped
	static HttpClient provideHttpClient() {
		return HttpClient.newHttpClient();
	}

	@Provides
	@FxApplicationScoped
	static HttpRequest provideCheckForUpdatesRequest(@Named("applicationVersion") Optional<String> applicationVersion) {
		String userAgent = String.format("Cryptomator VersionChecker/%s %s %s (%s)", //
				applicationVersion.orElse("SNAPSHOT"), //
				SystemUtils.OS_NAME, //
				SystemUtils.OS_VERSION, //
				SystemUtils.OS_ARCH); //
		return HttpRequest.newBuilder() //
				.uri(LATEST_VERSION_URI) //
				.header("User-Agent", userAgent) //
				.build();
	}

	@Provides
	@Named("checkForUpdatesInterval")
	@FxApplicationScoped
	static ObjectBinding<Duration> provideCheckForUpdateInterval(Settings settings) {
		return Bindings.when(settings.checkForUpdates()).then(UPDATE_CHECK_INTERVAL).otherwise(Duration.INDEFINITE);
	}

	@Provides
	@FxApplicationScoped
	static ScheduledService<String> provideCheckForUpdatesService(ExecutorService executor, HttpClient httpClient, HttpRequest checkForUpdatesRequest, @Named("checkForUpdatesInterval") ObjectBinding<Duration> period) {
		ScheduledService<String> service = new ScheduledService<>() {
			@Override
			protected Task<String> createTask() {
				return new UpdateCheckerTask(httpClient, checkForUpdatesRequest);
			}
		};
		service.setExecutor(executor);
		service.periodProperty().bind(period);
		return service;
	}


}
