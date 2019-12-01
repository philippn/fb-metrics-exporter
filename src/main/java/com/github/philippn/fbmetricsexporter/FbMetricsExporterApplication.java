/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.philippn.fbmetricsexporter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.github.kaklakariada.fritzbox.HomeAutomation;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * @author Philipp Nanz
 */
@SpringBootApplication
@EnableConfigurationProperties(FbProperties.class)
public class FbMetricsExporterApplication {

	public static void main(String[] args) {
		SpringApplication.run(FbMetricsExporterApplication.class, args);
	}

	@Bean
	public HomeAutomation homeAutomation(FbProperties properties) {
		return HomeAutomation.connect(properties.getBaseUrl(), properties.getUsername(), properties.getPassword());
	}

	@Bean
	public FbMetricsUpdater metricsUpdater(HomeAutomation homeAutomation, MeterRegistry meterRegistry) {
		return new FbMetricsUpdaterImpl(homeAutomation, meterRegistry);
	}
}
