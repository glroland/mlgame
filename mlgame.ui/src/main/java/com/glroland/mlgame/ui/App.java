package com.glroland.mlgame.ui;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Date;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

public class App {

	private static final int GAME_WIDTH = 15;
	private static final int GAME_HEIGHT = 15;
	private static final int GAME_DIFF = 3;
	private static final int BLOCK_SIZE = 10;
	private static final int DELAY = 250;
	private static final int WINDOW_WIDTH = GAME_WIDTH * BLOCK_SIZE;
	private static final int WINDOW_HEIGHT = GAME_HEIGHT * BLOCK_SIZE;
	private static final String SCREENSHOT_FORMAT = "JPG"; // Example: "PNG" or "JPG"

	// The window handle
	private long window;
	private Game game;

	public void run() {
		System.out.println("Hello LWJGL " + Version.getVersion() + "!");

		init();
		loop();

		// Free the window callbacks and destroy the window
		glfwFreeCallbacks(window);
		glfwDestroyWindow(window);

		// Terminate GLFW and free the error callback
		glfwTerminate();
		glfwSetErrorCallback(null).free();
	}

	private void init() {
		game = new Game(GAME_WIDTH, GAME_HEIGHT, GAME_DIFF);

		// Setup an error callback. The default implementation
		// will print the error message in System.err.
		GLFWErrorCallback.createPrint(System.err).set();

		// Initialize GLFW. Most GLFW functions will not work before doing this.
		if (!glfwInit())
			throw new IllegalStateException("Unable to initialize GLFW");

		// Configure GLFW
		glfwDefaultWindowHints(); // optional, the current window hints are already the default
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

		// Create the window
		window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, "mlgame", NULL, NULL);
		if (window == NULL)
			throw new RuntimeException("Failed to create the GLFW window");

		// Setup a key callback. It will be called every time a key is pressed, repeated
		// or released.
		glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
			if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
				glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
			else if (key == GLFW_KEY_LEFT && action == GLFW_PRESS) {
				logEvent("left");
				game.moveLeft();
			} else if (key == GLFW_KEY_RIGHT && action == GLFW_PRESS) {
				logEvent("right");
				game.moveRight();
			}
		});

		// Get the thread stack and push a new frame
		try (MemoryStack stack = stackPush()) {
			IntBuffer pWidth = stack.mallocInt(1); // int*
			IntBuffer pHeight = stack.mallocInt(1); // int*

			// Get the window size passed to glfwCreateWindow
			glfwGetWindowSize(window, pWidth, pHeight);

			// Get the resolution of the primary monitor
			GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

			// Center the window
			glfwSetWindowPos(window, (vidmode.width() - pWidth.get(0)) / 2, (vidmode.height() - pHeight.get(0)) / 2);
		} // the stack frame is popped automatically

		// Make the OpenGL context current
		glfwMakeContextCurrent(window);

		// Enable v-sync
		glfwSwapInterval(1);

		// Make the window visible
		glfwShowWindow(window);
	}

	private void drawGrid() {
		byte[][] b = game.getBoard();

		int h = game.getHeight();
		for (int y = 0; y < h; y++) {
			int w = game.getWidth();
			for (int x = 0; x < w; x++) {
				if (b[y][x] != 0)
					drawBlock(x, y);
			}
		}

		drawPlayer(game.getPlayerX());
	}

	private void logEvent(String action) {
		try {
			FileOutputStream fos = null;
			try
			{
				fos = new FileOutputStream("training_set.csv", true);
				PrintWriter pw = null;
				try
				{
					pw = new PrintWriter(fos);
					StringBuffer buf = new StringBuffer();
					buf.append(GAME_WIDTH).append(",");
					buf.append(GAME_HEIGHT).append(",");
					buf.append(GAME_DIFF).append(",");
					byte [][] board = game.getBoard();
					for (int y = 0; y < GAME_HEIGHT; y++)
					{
						for (int x = 0; x < GAME_WIDTH; x++)
						{
							buf.append(Byte.toString(board[y][x]));
						}
					}
					buf.append(",");
					buf.append(action);
					buf.append("\n");
					
					pw.append(buf.toString());
				}
				finally
				{
					if(pw != null)
						pw.close();
				}
			}
			finally
			{
				if (fos != null)
					fos.close();
			}
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void screenshot(String action) {
		GL11.glReadBuffer(GL11.GL_FRONT);
		int bpp = 4; // Assuming a 32-bit display with a byte each for red, green, blue, and alpha.
		ByteBuffer buffer = BufferUtils.createByteBuffer(WINDOW_WIDTH * WINDOW_HEIGHT * bpp);
		GL11.glReadPixels(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

		BufferedImage image = new BufferedImage(WINDOW_WIDTH, WINDOW_HEIGHT, BufferedImage.TYPE_INT_RGB);

		for (int x = 0; x < WINDOW_WIDTH; x++) {
			for (int y = 0; y < WINDOW_HEIGHT; y++) {
				int i = (x + (WINDOW_WIDTH * y)) * bpp;
				int r = buffer.get(i) & 0xFF;
				int g = buffer.get(i + 1) & 0xFF;
				int b = buffer.get(i + 2) & 0xFF;
				image.setRGB(x, WINDOW_HEIGHT - (y + 1), (0xFF << 24) | (r << 16) | (g << 8) | b);
			}
		}

		try {
			Date time = new Date();
			String filename = Long.toString(time.getTime()) + "_" + GAME_DIFF + "_" + action + "." + SCREENSHOT_FORMAT;
			File file = new File(filename);
			ImageIO.write(image, SCREENSHOT_FORMAT, file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void drawBlock(int x, int y) {
		byte[][] b = game.getBoard();

		GL11.glColor3f(0.0f, 0.0f, 1.0f);

		// draw quad
		int topLeftX = x * BLOCK_SIZE;
		int topLeftY = y * BLOCK_SIZE;
		int bottomRightX = ((x + 1) * BLOCK_SIZE) - 1;
		int bottomRightY = ((y + 1) * BLOCK_SIZE) - 1;

		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex2f(topLeftX, topLeftY);
		GL11.glVertex2f(bottomRightX, topLeftY);
		GL11.glVertex2f(bottomRightX, bottomRightY);
		GL11.glVertex2f(topLeftX, bottomRightY);
		GL11.glEnd();
	}

	private void drawPlayer(int x) {
		byte[][] b = game.getBoard();
		int y = game.getHeight() - 1;

		GL11.glColor3f(1.0f, 0.0f, 0.0f);

		// draw quad
		int topLeftX = x * BLOCK_SIZE;
		int topLeftY = y * BLOCK_SIZE;
		int bottomRightX = ((x + 1) * BLOCK_SIZE) - 1;
		int bottomRightY = ((y + 1) * BLOCK_SIZE) - 1;

		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex2f(topLeftX, topLeftY);
		GL11.glVertex2f(bottomRightX, topLeftY);
		GL11.glVertex2f(bottomRightX, bottomRightY);
		GL11.glVertex2f(topLeftX, bottomRightY);
		GL11.glEnd();
	}

	private void loop() {
		// This line is critical for LWJGL's interoperation with GLFW's
		// OpenGL context, or any context that is managed externally.
		// LWJGL detects the context that is current in the current thread,
		// creates the GLCapabilities instance and makes the OpenGL
		// bindings available for use.
		GL.createCapabilities();

		// Set the clear color
		glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

		// init OpenGL
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		GL11.glOrtho(0, BLOCK_SIZE * game.getWidth(), 0, BLOCK_SIZE * game.getHeight(), 1, -1);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);

		Date lastUpdate = new Date();

		// Run the rendering loop until the user has attempted to close
		// the window or has pressed the ESCAPE key.
		while (!glfwWindowShouldClose(window)) {

			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

			drawGrid();

			glfwSwapBuffers(window); // swap the color buffers

			// Poll for window events. The key callback above will only be
			// invoked during this call.
			glfwPollEvents();

			Date newUpdate = new Date();
			if (newUpdate.getTime() - lastUpdate.getTime() > DELAY) {
				lastUpdate = newUpdate;
				try {
					logEvent("noop");
					game.progressTime();
				} catch (Exception e) {
					System.exit(0);
				}
			}
		}
	}

	public static void main(String[] args) {
		new App().run();
	}

}