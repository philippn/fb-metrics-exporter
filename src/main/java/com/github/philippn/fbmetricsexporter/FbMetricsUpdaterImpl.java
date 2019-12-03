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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToDoubleFunction;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.kaklakariada.fritzbox.HomeAutomation;
import com.github.kaklakariada.fritzbox.model.homeautomation.Device;
import com.github.kaklakariada.fritzbox.model.homeautomation.DeviceList;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

/**
 * @author Philipp Nanz
 */
public class FbMetricsUpdaterImpl implements FbMetricsUpdater {

	private static final Logger logger = LoggerFactory.getLogger(FbMetricsUpdaterImpl.class);

	private HomeAutomation homeAutomation;
	private MeterRegistry meterRegistry;

	private AtomicInteger devicesTotal = new AtomicInteger();
	private AtomicInteger devicesPresent = new AtomicInteger();
	private Map<String, AtomicInteger> batteryByDevice = new HashMap<>();
	private Map<String, AtomicInteger> temperatureByDevice = new HashMap<>();
	private Map<String, AtomicInteger> hkrTistByDevice = new HashMap<>();
	private Map<String, AtomicInteger> hkrTsollByDevice = new HashMap<>();

	/**
	 * @param homeAutomation
	 * @param meterRegistry
	 */
	public FbMetricsUpdaterImpl(HomeAutomation homeAutomation, MeterRegistry meterRegistry) {
		this.homeAutomation = homeAutomation;
		this.meterRegistry = meterRegistry;
	}

	@PostConstruct
	public void init() {
		meterRegistry.gauge("fritzbox_devices_total", devicesTotal);
		meterRegistry.gauge("fritzbox_devices_present", devicesPresent);
	}

	@Override
	public void update() {
		DeviceList deviceList = homeAutomation.getDeviceListInfos();
		devicesTotal.set(deviceList.getDevices().size());
		devicesPresent.set(deviceList.getDevices().stream().mapToInt(d -> d.isPresent() ? 1 : 0).sum());
		for (Device device : deviceList.getDevices()) {
			if (!device.isPresent()) {
				continue;
			}
			// Battery
			AtomicInteger holder = getGaugeHolder(device, batteryByDevice, "fritzbox_device_battery_percent", null);
			holder.set(device.getBattery());
			logger.debug("Updated battery for device '{}' (id: {})", device.getName(), device.getId());
			// Temperature
			holder = getGaugeHolder(device, temperatureByDevice, "fritzbox_device_temperature_celsius", this::intBitsToDouble);
			holder.set(Float.floatToIntBits(device.getTemperature().getCelsius()));
			logger.debug("Updated temperature for device '{}' (id: {})", device.getName(), device.getId());
			// Hkr
			if (device.getHkr() != null) {
				// Tist
				holder = getGaugeHolder(device, hkrTistByDevice, "fritzbox_device_hkr_tist_celsius", this::fritzTempToDouble);
				holder.set(device.getHkr().getTist());
				logger.debug("Updated Tist for device '{}' (id: {})", device.getName(), device.getId());
				// Tist
				holder = getGaugeHolder(device, hkrTsollByDevice, "fritzbox_device_hkr_tsoll_celsius", this::fritzTempToDouble);
				holder.set(device.getHkr().getTsoll());
				logger.debug("Updated Tsoll for device '{}' (id: {})", device.getName(), device.getId());
			}
		}
	}

	private AtomicInteger getGaugeHolder(Device device, Map<String, AtomicInteger> map, String gaugeName, 
			ToDoubleFunction<AtomicInteger> valueFunction) {
		if (!map.containsKey(device.getId())) {
			AtomicInteger holder = new AtomicInteger();
			map.put(device.getId(), holder);
			Tags tags = Tags.of("name", device.getName());
			if (valueFunction != null) {
				meterRegistry.gauge(gaugeName, tags, holder, valueFunction);
			} else {
				meterRegistry.gauge(gaugeName, tags, holder);
			}
			logger.info("Registered gauge '{}' for device '{}' (id: {})", gaugeName, device.getName(), device.getId());
		}
		return map.get(device.getId());
	}

	private double intBitsToDouble(AtomicInteger bits) {
		float val = Float.intBitsToFloat(bits.get());
		return Float.valueOf(val).doubleValue();
	}

	private double fritzTempToDouble(AtomicInteger t) {
		return t.doubleValue() * 0.5;
	}
}
