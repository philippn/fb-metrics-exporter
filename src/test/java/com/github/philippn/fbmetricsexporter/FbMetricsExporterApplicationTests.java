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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import com.github.kaklakariada.fritzbox.HomeAutomation;
import com.github.kaklakariada.fritzbox.model.homeautomation.DeviceList;

import io.micrometer.core.instrument.MeterRegistry;

@SpringBootTest
@ActiveProfiles("test")
class FbMetricsExporterApplicationTests {

	@MockBean
	private HomeAutomation homeAutomation;
	@Autowired
	private MeterRegistry meterRegistry;
	@Autowired
	private FbMetricsUpdater metricsUpdater;

	@Test
	public void gaugesHaveBeenRegistered() throws Exception {
		Serializer serializer = new Persister();
		InputStream in = getClass().getResourceAsStream("devicelist.xml");
		DeviceList list = serializer.read(DeviceList.class, in);
		Mockito.when(homeAutomation.getDeviceListInfos()).thenReturn(list);
		
		metricsUpdater.update();
		
		assertEquals(2, meterRegistry.find("fritzbox_devices_total").gauge().value());
		assertEquals(2, meterRegistry.find("fritzbox_devices_present").gauge().value());
		assertEquals(2, meterRegistry.find("fritzbox_device_battery_percent").gauges().size());
		assertEquals(2, meterRegistry.find("fritzbox_device_temperature_celsius").gauges().size());
		assertEquals(2, meterRegistry.find("fritzbox_device_hkr_tist_celsius").gauges().size());
		assertEquals(2, meterRegistry.find("fritzbox_device_hkr_tsoll_celsius").gauges().size());
	}
}
