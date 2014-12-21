package orbotix.uisample;

import java.util.ArrayList;

import orbotix.robot.base.Robot;
import orbotix.robot.widgets.CalibrationImageButtonView;
import orbotix.robot.widgets.NoSpheroConnectedView;
import orbotix.robot.widgets.NoSpheroConnectedView.OnConnectButtonClickListener;
import orbotix.robot.widgets.SlideToSleepView;
import orbotix.sphero.ConnectionListener;
import orbotix.sphero.Sphero;
import orbotix.view.calibration.CalibrationView;
import orbotix.view.calibration.ControllerActivity;
import orbotix.view.connection.SpheroConnectionView;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

public class UiSampleActivity extends ControllerActivity {
	/** ID to start the StartupActivity for result to connect the Robot */
	private final static int STARTUP_ACTIVITY = 0;
	private static final int BLUETOOTH_ENABLE_REQUEST = 11;
	private static final int BLUETOOTH_SETTINGS_REQUEST = 12;

	/** The Robot to control */
	private Sphero mRobot;

	/** One-Touch Calibration Button */
	private CalibrationImageButtonView mCalibrationImageButtonView;

	/** Calibration View widget */
	private CalibrationView mCalibrationView;

	/** Slide to sleep view */
	private SlideToSleepView mSlideToSleepView;

	/** No Sphero Connected Pop-Up View */
	private NoSpheroConnectedView mNoSpheroConnectedView;

	private boolean[][] maze;
	private int entrance_x, entrance_y;
	private int exit_x, exit_y;

	private int current_x, current_y;

	private int mazeWidth;
	private int mazeHeight;

	final private int TOTAL_MAZES = 8; // number of maze files so far

	// 0 = up; 1 = right; 2 = down; 3 = left
	private ArrayList<Integer> steps;

	/** The Sphero Connection View */
	private SpheroConnectionView mSpheroConnectionView;

	/** Default Constructor */
	public UiSampleActivity() {
		super();
		// initializeMaze();
		loadMaze(0);
		steps = new ArrayList<Integer>();
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Set up the Sphero Connection View
		mSpheroConnectionView = (SpheroConnectionView) findViewById(R.id.sphero_connection_view);
		mSpheroConnectionView.addConnectionListener(new ConnectionListener() {

			@Override
			public void onConnected(Robot robot) {
				// Set Robot
				mRobot = (Sphero) robot; // safe to cast for now
				// Set connected Robot to the Controllers
				setRobot(mRobot);

				// Make sure you let the calibration view knows the robot it
				// should control
				mCalibrationView.setRobot(mRobot);

				// Make connect sphero pop-up invisible if it was previously up
				mNoSpheroConnectedView.setVisibility(View.GONE);
				mNoSpheroConnectedView.switchToConnectButton();
			}

			@Override
			public void onConnectionFailed(Robot sphero) {
				// let the SpheroConnectionView handle or hide it and do
				// something here...
			}

			@Override
			public void onDisconnected(Robot sphero) {
				mSpheroConnectionView.startDiscovery();
			}
		});

		// Add the calibration view
		mCalibrationView = (CalibrationView) findViewById(R.id.calibration_view);

		// Set up sleep view
		mSlideToSleepView = (SlideToSleepView) findViewById(R.id.slide_to_sleep_view);
		mSlideToSleepView.hide();
		// Send ball to sleep after completed widget movement
		mSlideToSleepView
				.setOnSleepListener(new SlideToSleepView.OnSleepListener() {
					@Override
					public void onSleep() {
						mRobot.sleep(0);
					}
				});

		// Initialize calibrate button view where the calibration circle shows
		// above button
		// This is the default behavior
		mCalibrationImageButtonView = (CalibrationImageButtonView) findViewById(R.id.calibration_image_button);
		mCalibrationImageButtonView.setCalibrationView(mCalibrationView);
		// You can also change the size and location of the calibration views
		// (or you can set it in XML)
		mCalibrationImageButtonView.setRadius(100);
		mCalibrationImageButtonView
				.setOrientation(CalibrationView.CalibrationCircleLocation.ABOVE);

		// Grab the No Sphero Connected View
		mNoSpheroConnectedView = (NoSpheroConnectedView) findViewById(R.id.no_sphero_connected_view);
		mNoSpheroConnectedView
				.setOnConnectButtonClickListener(new OnConnectButtonClickListener() {

					@Override
					public void onConnectClick() {
						mSpheroConnectionView.setVisibility(View.VISIBLE);
						mSpheroConnectionView.startDiscovery();
					}

					@Override
					public void onSettingsClick() {
						// Open the Bluetooth Settings Intent
						Intent settingsIntent = new Intent(
								Settings.ACTION_BLUETOOTH_SETTINGS);
						UiSampleActivity.this.startActivityForResult(
								settingsIntent, BLUETOOTH_SETTINGS_REQUEST);
					}
				});

	}

	/** Called when the user comes back to this app */
	@Override
	protected void onResume() {
		super.onResume();
		mSpheroConnectionView.startDiscovery();
	}

	/** Called when the user presses the back or home button */
	@Override
	protected void onPause() {
		super.onPause();
		if (mRobot != null) {
			mRobot.disconnect();
		}

	}

	@Override
	protected void onStop() {
		super.onStop();
		if (mRobot != null) {
			mRobot.disconnect();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK) {
			if (requestCode == BLUETOOTH_ENABLE_REQUEST) {
				// User enabled bluetooth, so refresh Sphero list
				mSpheroConnectionView.setVisibility(View.VISIBLE);
				mSpheroConnectionView.startDiscovery();
			}
		} else {
			if (requestCode == STARTUP_ACTIVITY) {
				// Failed to return any robot, so we bring up the no robot
				// connected view
				mNoSpheroConnectedView.setVisibility(View.VISIBLE);
			} else if (requestCode == BLUETOOTH_ENABLE_REQUEST) {

				// User clicked "NO" on bluetooth enable settings screen
				Toast.makeText(UiSampleActivity.this,
						"Enable Bluetooth to Connect to Sphero",
						Toast.LENGTH_LONG).show();
			} else if (requestCode == BLUETOOTH_SETTINGS_REQUEST) {
				// User enabled bluetooth, so refresh Sphero list
				mSpheroConnectionView.setVisibility(View.VISIBLE);
				mSpheroConnectionView.startDiscovery();
			}
		}
	}

	/**
	 * When the user clicks the "Sleep" button, show the SlideToSleepView shows
	 * 
	 * @param v
	 *            The Button clicked
	 */
	public void onSleepClick(View v) {
		mSlideToSleepView.show();
	}

	public void onMazeClick(View v) {
		int index = (int) (Math.random() * TOTAL_MAZES);
		loadMaze(index);
		Context context = getApplicationContext();
		CharSequence text = "Loaded Maze " + index + "!";
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(context, text, duration);
		toast.show();
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		mCalibrationView.interpretMotionEvent(event);
		mSlideToSleepView.interpretMotionEvent(event);
		return super.dispatchTouchEvent(event);
	}

	/*
	 * Read from a file representing a maze and convert it to the maze used in
	 * the program Precondition: Format of a maze file: mazeHeight:mazeWidth
	 * (height and width is separated by a colon, no spaces) (mazes here): make
	 * sure it is a mazeHeight by mazeWidth maze Symbols: - X: obstacle - O:
	 * walkable - E: entrance - T: terminal/exit
	 */
	public void loadMaze(int index) {

		// randomly generate a number that links to a file representing a maze
		// int index = (int) (Math.random() * TOTAL_MAZE()

		String m;
		switch (index) {
		case 0:
			m = Mazes.MAZE0;
			mazeHeight = Mazes.MAZE0_H;
			mazeWidth = Mazes.MAZE0_W;
			break;
		case 1:
			m = Mazes.MAZE1;
			mazeHeight = Mazes.MAZE1_H;
			mazeWidth = Mazes.MAZE1_W;
			break;
		case 2:
			m = Mazes.MAZE2;
			mazeHeight = Mazes.MAZE2_H;
			mazeWidth = Mazes.MAZE2_W;
			break;
		case 3:
			m = Mazes.MAZE3;
			mazeHeight = Mazes.MAZE3_H;
			mazeWidth = Mazes.MAZE3_W;
			break;
		case 4:
			m = Mazes.MAZE4;
			mazeHeight = Mazes.MAZE4_H;
			mazeWidth = Mazes.MAZE4_W;
			break;
		case 5:
			m = Mazes.MAZE5;
			mazeHeight = Mazes.MAZE5_H;
			mazeWidth = Mazes.MAZE5_W;
			break;
		case 6:
			m = Mazes.MAZE6;
			mazeHeight = Mazes.MAZE6_H;
			mazeWidth = Mazes.MAZE6_W;
			break;
		case 7:
			m = Mazes.MAZE7;
			mazeHeight = Mazes.MAZE7_H;
			mazeWidth = Mazes.MAZE7_W;
			break;
		default:
			m = "";
		}

		maze = new boolean[mazeHeight][mazeWidth];

		for (int i = 0; i < mazeHeight; i++) {
			for (int j = 0; j < mazeWidth; j++) {
				char c = m.charAt(i * mazeWidth + j);
				if (c == 'E' || c == 'T' || c == 'O') {
					maze[i][j] = true;
					if (c == 'E') {
						entrance_x = j;
						entrance_y = i;
						current_x = entrance_x;
						current_y = entrance_y;
					} else if (c == 'T') {
						exit_x = j;
						exit_y = i;
					}
				}
			}
		}

	}

	public synchronized void undo() {
		int temp;
		float heading;
		float speed = 0.6f;
		if (steps.size() > 0) {
			temp = steps.get(steps.size() - 1);
			if (temp == 2) {
				heading = 0f;
				current_y--;
			} else if (temp == 3) {
				heading = 90f;
				current_x++;
			} else if (temp == 0) {
				heading = 180f;
				current_y--;
			} else {
				heading = 270f;
				current_x--;
			}

			steps.remove(steps.size() - 1);

			mRobot.rotate(heading);
			try {
				Thread.sleep(500);
			} catch (Exception e) {
			}

			mRobot.drive(heading, speed);
			final Handler handler = new Handler();
			handler.postDelayed(new Runnable() {

				@Override
				public void run() {
					mRobot.stop();
				}
			}, 100);
		} else {
			blinkRed();
		}
	}

	public void reset() {
		while (steps.size() > 0)
			undo();
	}

	public void blinkRed() {
		mRobot.setColor(255, 0, 0);
		// Send a delayed message on a handler
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {

			@Override
			public void run() {
				mRobot.setColor(255, 255, 255);
			}
		}, 1000);
		// mRobot.setColor(red, green, blue);

	}

	/**
	 * When the user clicks a control button, roll the Robot in that direction
	 * 
	 * @param v
	 *            The View that had been clicked
	 */
	public synchronized void onDirectionClick(View v) {
		// Find the heading, based on which button was clicked
		final float heading;
		// Set speed. 60% of full speed
		final float speed;

		switch (v.getId()) {

		case R.id.right_button:
			// right
			heading = 90f;
			if (current_x + 1 < mazeWidth && maze[current_y][current_x + 1]) {
				current_x++;
				steps.add(1);
				speed = 0.6f;

			} else {
				speed = 0f;
				blinkRed();
			}
			break;

		case R.id.up_button:
			// up
			heading = 0f;
			if (current_y - 1 >= 0 && maze[current_y - 1][current_x]) {
				current_y--;
				steps.add(0);
				// Roll robot
				speed = 0.6f;

			} else {
				speed = 0f;
				blinkRed();
			}
			break;

		case R.id.left_button:
			// left
			heading = 270f;
			if (current_x - 1 >= 0 && maze[current_y][current_x - 1]) {
				current_x--;
				steps.add(3);
				speed = 0.6f;
			} else {
				speed = 0f;
				blinkRed();
			}
			break;

		case R.id.down_button:
			// down
			heading = 180f;
			if (current_y + 1 < mazeHeight && maze[current_y + 1][current_x]) {
				current_y++;
				steps.add(2);
				speed = 0.6f;
			} else {
				speed = 0f;
				blinkRed();
			}
			break;
		default:
			heading = 0f;
			speed = 0f;
			undo();
			return;
		}

		Handler handler = new Handler();

		// Roll robot
		mRobot.rotate(heading);
		try {
			Thread.sleep(500);
		} catch (Exception e) {
		}
		mRobot.drive(heading, speed);

		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				mRobot.stop();
			}
		}, 100);

		if (current_x == exit_x && current_y == exit_y) {
			mRobot.setColor(0, 255, 0);
		}
	}
}
