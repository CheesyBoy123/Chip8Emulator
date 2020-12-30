import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.FileInputStream;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class Emulator extends JPanel implements KeyListener {

	private static int scale = 20;
	public static boolean DEBUG = false;
	public static int doXClockCycles = 0;

	public Bus bus;

	public Emulator(Bus bus) {
		this.bus = bus;
	}

	public static void main(String[] args) {
		Emulator em = new Emulator(new Bus());
		System.out.println("Starting up emulation...");
		JFrame window = new JFrame();
		// each "pixel" is actually 10 pixels.
		window.setSize(65 * scale, 33 * scale);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.add(em);
		window.setResizable(false);
		window.setFocusable(true);
		window.requestFocusInWindow();
		window.add(em);
		window.addKeyListener(em);
		window.setVisible(true);

		em.start("./rsc/1dcell.ch8");

	}

	public void start(String ROM) {
		// load our ROM file into memory.
		FileInputStream in = null;
		try {
			in = new FileInputStream(ROM);
			// starting at 512.
			int currentPointer = 512;
			int c;
			// prime our memory.
			while ((c = in.read()) != -1) {
				bus.write(currentPointer, (byte) c);
				currentPointer++;
			}
			bus.cpu.PC = 512;

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}

		System.out.println("Finished reading in ROM...");

		// CPU cycles
		int cycles = 0;
		// FPS
		int screenCycles = 0;
		// The amount of CPU cycles we need to have 60 fps.
		int neededScreenCycles = Chip8.PROCESSOR_SPEED / 60;

		long initTime;
		long endTime;

		// Begin our emulation
		while (true) {
			initTime = System.nanoTime();

			if (DEBUG) {

				if (doXClockCycles > 0) {
					doXClockCycles--;
					bus.clock();

					// We are able to draw a frame now.
					if (screenCycles % neededScreenCycles == 0) {
						screenCycles = 0;
						// We have something to draw.
						if (bus.cpu.drawScreen) {
							repaint();
							bus.cpu.drawScreen = false;
						}

						bus.cpu.newFrame();

					}

					endTime = System.nanoTime();
					screenCycles++;

					// We need to wait for things to catch back up.

					endTime = System.nanoTime();
					cycles++;

					waitCompletedCycle(endTime, initTime);

					if (cycles == Chip8.PROCESSOR_SPEED) {
						cycles = 0;
					}
				}

				continue;
			}

			// Clock the bus.
			bus.clock();

			// We are able to draw a frame now.
			if (screenCycles % neededScreenCycles == 0) {
				screenCycles = 0;
				// We have something to draw.
				if (bus.cpu.drawScreen) {
					repaint();
					bus.cpu.drawScreen = false;
				}

				bus.cpu.newFrame();

			}

			endTime = System.nanoTime();
			screenCycles++;

			// We need to wait for things to catch back up.

			endTime = System.nanoTime();
			cycles++;

			waitCompletedCycle(endTime, initTime);

			if (cycles == Chip8.PROCESSOR_SPEED) {
				cycles = 0;
			}

		}

	}

	public void waitCompletedCycle(long endTime, long initTime) {
		long singleCycle = 1000000000 / Chip8.PROCESSOR_SPEED;
		long currentTime = System.nanoTime();
		long cycleFinishTime = currentTime + singleCycle - (endTime - initTime);

		while (System.nanoTime() < cycleFinishTime) {
			try {
				Thread.sleep(0);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	public void paintComponent(Graphics g) {

		super.paintComponent(g);
		for (int x = 0; x < 64; x++) {
			for (int y = 0; y < 32; y++) {
				if (bus.display[x][y]) {
					g.setColor(Color.WHITE);
					g.fillRect(x * scale, y * scale, scale, scale);
				} else {
					g.setColor(Color.BLACK);
					g.fillRect(x * scale, y * scale, scale, scale);
				}
			}
		}

		// just draw some lines
		g.setColor(Color.BLACK);
		for (int x = 0; x < 64; x++) {
			g.drawLine(x * scale, 0, x * scale, 32 * scale);
		}

		for (int y = 0; y < 32; y++) {
			g.drawLine(0, y * scale, 64 * scale, y * scale);
		}

	}

	@Override
	public void keyTyped(KeyEvent e) {
		bus.keyboard.keyTyped(e);
	}

	@Override
	public void keyPressed(KeyEvent e) {
		bus.keyboard.keyPressed(e);
	}

	@Override
	public void keyReleased(KeyEvent e) {
		bus.keyboard.keyReleased(e);
	}

}
