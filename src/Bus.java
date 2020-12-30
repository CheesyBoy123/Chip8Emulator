
public class Bus {

	public Bus() {

		// clear our RAM
		for (int i = 0; i < 4096; i++)
			RAM[i] = 0x00;

		// clear our screen
		for (int i = 0; i < 64; i++)
			for (int j = 0; j < 32; j++)
				display[i][j] = false;

		cpu = new Chip8();
		keyboard = new Keyboard();
		cpu.initializeChip(this);
	}

	// our display, either pixels are on or they are off.
	// Our display is 64x32.
	public boolean[][] display = new boolean[64][32];

	// 4k RAM
	public byte[] RAM = new byte[4096];
	// Our CPU chip.
	public Chip8 cpu;
	// Our keyboard
	public Keyboard keyboard;

	public void clock() {
		cpu.clock();
	}

	public void write(int addr, byte data) {
		if (addr >= 0x000 && addr <= 0xFFF) {
			RAM[addr] = data;
		} else {
			System.out.println("Out of bounds for memory... Ignoring. [" + addr + ", " + cpu.PC + " "
					+ cpu.STACK_POINTER + " " + cpu.I_REGISTER + "]");
		}
	}

	public byte read(int addr) {
		byte data = 0x000;

		if (addr >= 0x000 && addr <= 0xFFF)
			data = RAM[addr];

		return data;
	}

}
