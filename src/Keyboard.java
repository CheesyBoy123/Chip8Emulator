import java.awt.event.KeyEvent;

public class Keyboard {

	public boolean[] mappedKeys = new boolean[16];

	public void keyTyped(KeyEvent e) {

	}

	public void keyPressed(KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_B:
			Emulator.DEBUG = !Emulator.DEBUG;
			System.out.println("DEBUG: " + Emulator.DEBUG);
			break;
		case KeyEvent.VK_N:
			Emulator.doXClockCycles++;
			break;

		case KeyEvent.VK_1:
			mappedKeys[0x1] = true;
			break;
		case KeyEvent.VK_2:
			mappedKeys[0x2] = true;
			break;
		case KeyEvent.VK_3:
			mappedKeys[0x3] = true;
			break;
		case KeyEvent.VK_4:
			mappedKeys[0xC] = true;
			break;
		case KeyEvent.VK_Q:
			mappedKeys[0x4] = true;
			break;
		case KeyEvent.VK_W:
			mappedKeys[0x5] = true;
			break;
		case KeyEvent.VK_E:
			mappedKeys[0x6] = true;
			break;
		case KeyEvent.VK_R:
			mappedKeys[0xD] = true;
			break;
		case KeyEvent.VK_A:
			mappedKeys[0x7] = true;
			break;
		case KeyEvent.VK_S:
			mappedKeys[0x8] = true;
			break;
		case KeyEvent.VK_D:
			mappedKeys[0x9] = true;
			break;
		case KeyEvent.VK_F:
			mappedKeys[0xE] = true;
			break;
		case KeyEvent.VK_Z:
			mappedKeys[0xA] = true;
			break;
		case KeyEvent.VK_X:
			mappedKeys[0x0] = true;
			break;
		case KeyEvent.VK_C:
			mappedKeys[0xB] = true;
			break;
		case KeyEvent.VK_V:
			mappedKeys[0xF] = true;
			break;
		default:
			break;
		}
	}

	public void keyReleased(KeyEvent e) {

		switch (e.getKeyCode()) {
		case KeyEvent.VK_1:
			mappedKeys[0x1] = false;
			break;
		case KeyEvent.VK_2:
			mappedKeys[0x2] = false;
			break;
		case KeyEvent.VK_3:
			mappedKeys[0x3] = false;
			break;
		case KeyEvent.VK_4:
			mappedKeys[0xC] = false;
			break;
		case KeyEvent.VK_Q:
			mappedKeys[0x4] = false;
			break;
		case KeyEvent.VK_W:
			mappedKeys[0x5] = false;
			break;
		case KeyEvent.VK_E:
			mappedKeys[0x6] = false;
			break;
		case KeyEvent.VK_R:
			mappedKeys[0xD] = false;
			break;
		case KeyEvent.VK_A:
			mappedKeys[0x7] = false;
			break;
		case KeyEvent.VK_S:
			mappedKeys[0x8] = false;
			break;
		case KeyEvent.VK_D:
			mappedKeys[0x9] = false;
			break;
		case KeyEvent.VK_F:
			mappedKeys[0xE] = false;
			break;
		case KeyEvent.VK_Z:
			mappedKeys[0xA] = false;
			break;
		case KeyEvent.VK_X:
			mappedKeys[0x0] = false;
			break;
		case KeyEvent.VK_C:
			mappedKeys[0xB] = false;
			break;
		case KeyEvent.VK_V:
			mappedKeys[0xF] = false;
			break;
		default:
			break;
		}
	}

}
