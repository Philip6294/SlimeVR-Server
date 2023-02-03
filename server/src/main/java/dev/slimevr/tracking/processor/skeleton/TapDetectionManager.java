package dev.slimevr.tracking.processor.skeleton;


import dev.slimevr.config.TapDetectionConfig;
import dev.slimevr.osc.VRCOSCHandler;
import dev.slimevr.tracking.trackers.Tracker;
import io.eiren.util.logging.LogManager;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.BufferedInputStream;
import java.util.Objects;


// handles tap detection for the skeleton
public class TapDetectionManager {

	// server and related classes
	private HumanSkeleton skeleton;
	private VRCOSCHandler oscHandler;
	private TapDetectionConfig config;

	// tap detectors
	private TapDetection quickResetDetector;
	private TapDetection resetDetector;
	private TapDetection mountingResetDetector;

	// number of taps to detect
	private int quickResetTaps = 2;
	private int resetTaps = 3;
	private int mountingResetTaps = 3;

	// delay
	private static final float NS_CONVERTER = 1.0e9f;
	private float resetDelayNs = 0.20f * NS_CONVERTER;
	private float quickResetDelayNs = 1.00f * NS_CONVERTER;
	private float mountingResetDelayNs = 1.00f * NS_CONVERTER;

	private boolean quickResetNotFired = true;
	private boolean resetNotFired = true;
	private boolean mountingResetNotFired = true;

	public TapDetectionManager(HumanSkeleton skeleton) {
		this.skeleton = skeleton;
	}

	public TapDetectionManager(
		HumanSkeleton skeleton,
		VRCOSCHandler oscHandler,
		TapDetectionConfig config
	) {
		this.skeleton = skeleton;
		this.oscHandler = oscHandler;
		this.config = config;

		quickResetDetector = new TapDetection(skeleton, getTrackerToWatchQuickReset());
		resetDetector = new TapDetection(skeleton, getTrackerToWatchReset());
		mountingResetDetector = new TapDetection(skeleton, getTrackerToWatchMountingReset());

		// since this config value is only modified by editing the config file,
		// we can set it here
		quickResetDetector
			.setNumberTrackersOverThreshold(
				config.getNumberTrackersOverThreshold()
			);
		resetDetector
			.setNumberTrackersOverThreshold(
				config.getNumberTrackersOverThreshold()
			);
		mountingResetDetector
			.setNumberTrackersOverThreshold(
				config.getNumberTrackersOverThreshold()
			);

		updateConfig();
	}

	public void updateConfig() {
		this.quickResetDelayNs = config.getQuickResetDelay() * NS_CONVERTER;
		this.resetDelayNs = config.getResetDelay() * NS_CONVERTER;
		this.mountingResetDelayNs = config.getMountingResetDelay() * NS_CONVERTER;
		quickResetDetector.setEnabled(config.getQuickResetEnabled());
		resetDetector.setEnabled(config.getResetEnabled());
		mountingResetDetector.setEnabled(config.getMountingResetEnabled());
		quickResetTaps = config.getQuickResetTaps();
		resetTaps = config.getResetTaps();
		mountingResetTaps = config.getMountingResetTaps();
		quickResetDetector.setMaxTaps(quickResetTaps);
		resetDetector.setMaxTaps(resetTaps);
		mountingResetDetector.setMaxTaps(mountingResetTaps);
	}

	public void update() {
		if (quickResetDetector == null || resetDetector == null || mountingResetDetector == null)
			return;
		// update the tap detectors
		quickResetDetector.update();
		resetDetector.update();
		mountingResetDetector.update();

		// check if any tap detectors have detected taps
		checkQuickReset();
		checkReset();
		checkMountingReset();
	}

	private void checkQuickReset() {
		boolean tapped = (quickResetTaps <= quickResetDetector.getTaps());

		if (tapped && quickResetNotFired) {
			playSound(0);
			quickResetNotFired = false;
		}

		if (
			tapped && System.nanoTime() - quickResetDetector.getDetectionTime() > quickResetDelayNs
		) {
			if (oscHandler != null)
				oscHandler.yawAlign();
			LogManager.debug("Tap Quick Reset");
			skeleton.resetTrackersYaw();
			quickResetDetector.resetDetector();
			quickResetNotFired = true;
		}
	}

	private void checkReset() {
		boolean tapped = (resetTaps <= resetDetector.getTaps());

		if (tapped && resetNotFired) {
			playSound(1);
			resetNotFired = false;
		}

		if (
			tapped && System.nanoTime() - resetDetector.getDetectionTime() > resetDelayNs
		) {
			if (oscHandler != null)
				oscHandler.yawAlign();
			LogManager.debug("Tap Reset");
			skeleton.resetTrackersFull();
			resetDetector.resetDetector();
			resetNotFired = true;
		}
	}

	private void checkMountingReset() {
		boolean tapped = (mountingResetTaps <= mountingResetDetector.getTaps());

		if (tapped && mountingResetNotFired) {
			playSound(2);
			mountingResetNotFired = false;
		}

		if (
			tapped
				&& System.nanoTime() - mountingResetDetector.getDetectionTime()
					> mountingResetDelayNs
		) {
			LogManager.debug("Tap Mounting Reset");
			skeleton.resetTrackersMounting();
			mountingResetDetector.resetDetector();
			mountingResetNotFired = true;
		}
	}

	// returns either the chest tracker, hip tracker, or waist tracker depending
	// on which one is available
	// if none are available, returns null

	private Tracker getTrackerToWatchQuickReset() {
		if (skeleton.chestTracker != null)
			return skeleton.chestTracker;
		else if (skeleton.hipTracker != null)
			return skeleton.hipTracker;
		else if (skeleton.waistTracker != null)
			return skeleton.waistTracker;
		else
			return null;
	}

	private Tracker getTrackerToWatchReset() {
		if (skeleton.leftUpperLegTracker != null)
			return skeleton.leftUpperLegTracker;
		else if (skeleton.leftLowerLegTracker != null)
			return skeleton.leftLowerLegTracker;
		return null;
	}

	private Tracker getTrackerToWatchMountingReset() {
		if (skeleton.rightUpperLegTracker != null)
			return skeleton.rightUpperLegTracker;
		else if (skeleton.rightLowerLegTracker != null)
			return skeleton.rightLowerLegTracker;
		return null;
	}


	private void playSound(int i) {
		new Thread(new Runnable() {

			public void run() {
				String soundName = switch (i) {
					default -> "/beep.wav"; // default implies 0
					case 1 -> "/double beep.wav";
					case 2 -> "/triple beep.wav";
				};

				try {
					Clip clip = AudioSystem.getClip();

					BufferedInputStream bufferedStream = new BufferedInputStream(
						Objects.requireNonNull(this.getClass().getResourceAsStream(soundName))
					);
					AudioInputStream inputStream = AudioSystem.getAudioInputStream(bufferedStream);

					clip.open(inputStream);
					clip.start();

				} catch (Exception e) {
					System.err.println(e.getMessage());
					e.printStackTrace();
				}
			}
		}).start();
	}
}
