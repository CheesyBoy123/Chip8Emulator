import java.util.Random;

/***
 * 
 * https://tobiasvl.github.io/blog/write-a-chip-8-emulator/#history
 *
 * For testing OP-Codes: https://github.com/corax89/chip8-test-rom Passed all
 * tests!
 * 
 * BC_test.ch8 also passed all tests!
 */

public class Chip8 {
	// This is our simulated processor speed (in Hz)
	public static int PROCESSOR_SPEED = 3000;

	// Our bus
	public Bus bus;

	// our program counter, we always start at 512, the first 512 bytes are used for
	// the interpreter and fonts
	int PC = 512;
	// our 16-bit I(ndex) register
	short I_REGISTER = 0;
	// 16-bit Stack pointer, points to somewhere on our RAM.
	// We store it at the end of the RAM, 16 address spaces.
	int STACK_POINTER = 4047;
	// 8-bit delay timer (decremented at a rate of 60Hz)
	byte DELAY_TIMER = 60;
	// 8-bit sound timer, functions like a delay timer; but is also meant to beep
	// when not equal to 0.
	byte SOUND_TIMER = 60;
	// 16 8-Bit general purpose registers 0-F
	// V_REGISTERS[0xF] is used as a flag register, some instructions will set this
	// to 1 or 0 based on rules.
	// For example, it can be used as a carry bit.
	byte[] V_REGISTERS = new byte[16];

	// Should we redraw the screen.
	boolean drawScreen = false;

	// count this as 1 clock cycle, we will simulate at ~600Hz so every 10 clock
	// cycles
	// we decriment our delay & sound timer.
	public void clock() {

		// Instructions are 2 bytes, but we can only retrieve 1 byte at a time
		// in memory.
		byte instr_1 = fetch();
		PC++;
		byte instr_2 = fetch();
		PC++;

		// Combine our instruction
		short combined = (short) (((instr_1 << 8) | (instr_2 & 0xFF)) & 0xFFFF);

		if (Emulator.DEBUG) {
			System.out.println("Current Instruction being executed: " + Integer.toHexString(combined));
			System.out.println("Stack Pointer: " + STACK_POINTER);
			System.out.println("New PC: " + PC);
			System.out.println("I Register: " + I_REGISTER);

			for (int i = 0; i < 16; i++) {
				System.out.println("V_" + i + " Register: " + Integer.toHexString(V_REGISTERS[i]));
			}

		}

		// decode and execute our instruction.
		decodeInstruction(combined);

	}

	public void decodeInstruction(Short inst) {
		if (inst == 0x00E0) { // Clear screen
			clearScreen();
			// this is a special case instruction.
			return;
		} else if ((inst != (short) 0x00EE) && (inst >= (short) 0x0000 && inst <= (short) 0x0FFF)) { // Execute machine
																										// language
																										// routine
			executeMachineLanguage(inst);
		} else if (inst >= (short) 0x1000 && inst <= (short) 0x1FFF) { // Jump
			jump(inst);
		} else if (inst == (short) 0x00EE || (inst >= (short) 0x2000 && inst <= (short) 0x2FFF)) { // Push/Pop PCs to
																									// our stack.
			doSubroutine(inst);
		} else if (inst >= (short) 0x3000 && inst <= (short) 0x3FFF) { // check if a register equals a value
			if (checkIfRegisterEqualsValue(inst)) {
				PC += 2;
			}
		} else if (inst >= (short) 0x4000 && inst <= (short) 0x4FFF) { // check if a register DOESN'T equal a value.
			if (!checkIfRegisterEqualsValue(inst)) {
				PC += 2;
			}
		} else if (inst >= (short) 0x5000 && inst <= (short) 0x5FFF) { // check if 2 registers have the SAME value
			if (checkIfRegisterEqualsRegister(inst)) {
				PC += 2;
			}
		} else if (inst >= (short) 0x6000 && inst <= (short) 0x6FFF) { // Set register V_X
			setVRegister(inst);
		} else if (inst >= (short) 0x7000 && inst <= (short) 0x7FFF) { // Add some amount to register V_X
			addVRegister(inst);
		} else if (inst >= (short) 0x8000 && inst <= (short) 0x8FFF) { // Do our logical arithmetic
			doLogicInstructios(inst);
		} else if (inst >= (short) 0x9000 && inst <= (short) 0x9FFF) { // Check if 2 registers DONT have the SAME value
			if (!checkIfRegisterEqualsRegister(inst)) {
				PC += 2;
			}
		} else if (inst >= (short) 0xA000 && inst <= (short) 0xAFFF) { // Set index register I
			setIRegister(inst);
		} else if (inst >= (short) 0xB000 && inst <= (short) 0xBFFF) { // Jump with offset
			jumpWithOffset(inst);
		} else if (inst >= (short) 0xC000 && inst <= (short) 0xCFFF) { // Generate random number
			random(inst);
		} else if (inst >= (short) 0xd000 && inst <= (short) 0xdfff) { // display/draw
			display(inst);
		} else if (inst >= (short) 0xE000 && inst <= (short) 0xEFFF) { // Check if keys are pressed or not (and skip
																		// inst if so)
			if (checkKeys(inst))
				PC += 2;
		} else if (inst >= (short) 0xF000 && inst <= (short) 0xFFFF) { // do some misc stuff.
			doMiscItems(inst);
		}

	}

	public byte fetch() {
		return bus.read(PC);
	}

	// This can be skipped. Considered NOOP
	private void executeMachineLanguage(Short inst) {
		if (Emulator.DEBUG)
			System.exit(0);
	}

	private void doMiscItems(Short inst) {
		int type = inst & 0xFF;
		int x = (inst & 0xF00) >> 8;
		if (type == 7) {
			V_REGISTERS[x] = DELAY_TIMER;
		} else if (type == 21) {
			DELAY_TIMER = V_REGISTERS[x];
		} else if (type == 24) {
			SOUND_TIMER = V_REGISTERS[x];
		} else if (type == 30) { // passed
			I_REGISTER += V_REGISTERS[x];
		} else if (type == 10) {
			boolean skip = false;
			for (int i = 0; i < 16; i++) {
				if (bus.keyboard.mappedKeys[i]) {
					skip = true;
					V_REGISTERS[x] = (byte) (i & 0xFF);
					break;
				}
			}

			if (!skip)
				PC -= 2;

		} else if (type == 41) {
			int nibbleOffset = (inst & 0xF00) >> 8;
			int ch = V_REGISTERS[nibbleOffset] & 0xF;
			I_REGISTER = (short) (50 + (5 * ch));
		} else if (type == 51) {

			int val = V_REGISTERS[x] & 0xFF;
			int hundreds = val / 100;
			val = val - hundreds * 100;
			int tens = val / 10;
			val = val - tens * 10;

			bus.write(I_REGISTER, (byte) hundreds);
			bus.write(I_REGISTER + 1, (byte) tens);
			bus.write(I_REGISTER + 2, (byte) val);

		} else if (type == 85) {

			for (int i = 0; i <= x; i++) {
				bus.write((I_REGISTER + i), V_REGISTERS[i]);
			}

		} else if (type == 101) {
			for (int i = 0; i <= x; i++) {
				V_REGISTERS[i] = bus.read((I_REGISTER + i));
			}
		}
	}

	// Check if a specific key is being held.
	// [PASSED]
	private boolean checkKeys(Short inst) {
		short type = (short) (inst & 0x00FF);
		if (type == (short) 0x9E) {
			return bus.keyboard.mappedKeys[V_REGISTERS[(inst & 0x0F00) >> 8]];
		} else if (type == (short) 0xA1) {
			return !bus.keyboard.mappedKeys[V_REGISTERS[(inst & 0x0F00) >> 8]];
		}
		return false;
	}

	// Generate a random number mod 0x00NN
	// [PASSED]
	private void random(Short inst) {
		short r = (short) new Random().nextInt();
		r = (short) (r & (inst & 0x00FF));

		V_REGISTERS[(inst & 0x0F00) >> 8] = (byte) r;
	}

	// Jump to PC = NNN + V_0 as an offset.
	private void jumpWithOffset(Short inst) {
		PC = V_REGISTERS[0] + (inst & 0x0FFF);
	}

	private void doLogicInstructios(Short inst) {
		int type = inst & 0xF;
		int x = (inst & 0xF00) >> 8;
		int y = (inst & 0xF0) >> 4;
		if (type == 0) {
			V_REGISTERS[x] = V_REGISTERS[y];
		} else if (type == 1) {
			V_REGISTERS[x] = (byte) (V_REGISTERS[x] | V_REGISTERS[y]);
		} else if (type == 2) {
			V_REGISTERS[x] = (byte) (V_REGISTERS[x] & V_REGISTERS[y]);
		} else if (type == 3) {
			V_REGISTERS[x] = (byte) (V_REGISTERS[x] ^ V_REGISTERS[y]);
		} else if (type == 4) {
			int before = V_REGISTERS[x] & 0x8000;
			int added = V_REGISTERS[y] & 0x8000;

			V_REGISTERS[x] += V_REGISTERS[y];

			int r = V_REGISTERS[x] & 0x8000;

			V_REGISTERS[0xF] = (byte) (~(before ^ added) & (before ^ r));

		} else if (type == 5) {

			int Vx = V_REGISTERS[x] & 0xFF;
			int Vy = V_REGISTERS[y] & 0xFF;

			int r = Vx - Vy;
			V_REGISTERS[0xF] = (byte) (r < 0 ? 0 : 1);
			V_REGISTERS[x] = (byte) (r & 0xFF);

		} else if (type == 6) {
//			V_REGISTERS[x] = V_REGISTERS[y];
			V_REGISTERS[0xF] = (byte) (V_REGISTERS[x] & 0x1);
			V_REGISTERS[x] >>= 1;
		} else if (type == 7) {

			int Vx = V_REGISTERS[x] & 0xFF;
			int Vy = V_REGISTERS[y] & 0xFF;
			int r = Vy - Vx;
			V_REGISTERS[0xF] = (byte) (r < 0 ? 0 : 1);
			V_REGISTERS[x] = (byte) (r & 0xFF);

		} else if (type == 14) {
//			V_REGISTERS[x] = V_REGISTERS[y];
			V_REGISTERS[0xF] = (byte) ((V_REGISTERS[x] & 0x80) >> 7);
			V_REGISTERS[x] <<= 1;
		}

	}

	// Determine if two registers are equal
	// [PASSED]
	private boolean checkIfRegisterEqualsRegister(Short inst) {
		int x = V_REGISTERS[(inst & 0x0F00) >> 8] & 0xFF;
		int y = V_REGISTERS[(inst & 0x00F0) >> 4] & 0xFF;
		return x == y;
	}

	// Determine if Register X is equal to a value.
	// [PASSED]
	private boolean checkIfRegisterEqualsValue(Short inst) {
		int x = V_REGISTERS[(inst & 0x0F00) >> 8] & 0xFF;
		return x == (int) (inst & 0x00FF);
	}

	// Our instruction functions!
	// [PASSED]
	public void clearScreen() {
		for (int i = 0; i < 64; i++) {
			for (int j = 0; j < 32; j++) {
				bus.display[i][j] = false;
			}
		}

		drawScreen = true;
	}

	// Jump to a different portion of memory.
	// [PASSED]
	public void jump(short inst) {
		// Mask off the 0x1 and set the program counter.
		PC = (inst & 0x0FFF);
	}

	// Set a register's value
	// [PASSED]
	public void setVRegister(short inst) {
		V_REGISTERS[(inst & 0x0F00) >> 8] = (byte) (inst & 0x00FF);
	}

	// Set the I Register's value
	// [PASSED]
	public void setIRegister(short inst) {
		I_REGISTER = (short) (inst & 0x0FFF);
	}

	// Add a value to one of our V Register's
	// [PASSED]
	public void addVRegister(short inst) {
		int before = V_REGISTERS[(inst & 0x0F00) >> 8] & 0x8000;
		int added = (byte) (inst & 0x00FF) & 0x8000;

		V_REGISTERS[(inst & 0x0F00) >> 8] += (byte) (inst & 0x00FF);

		int r = V_REGISTERS[(inst & 0x0F00) >> 8] & 0x8000;

		V_REGISTERS[0xF] = (byte) (~(before ^ added) & (before ^ r));
	}

	// Do a function subroutine,
	// 0x00EE means "return"
	// 0x2NNN Means jump to function at mem addr 0xNNN
	//
	public void doSubroutine(short inst) {
		// our instructions can be up to 2 bytes, so we need to push/pop 2 bytes from
		// the stack each time.
		if (inst == (short) 0x00EE) {
			STACK_POINTER--;
			int loByte = (int) (bus.read(STACK_POINTER) & 0x000000FF);
			STACK_POINTER--;
			int highByte = (int) (bus.read(STACK_POINTER) & 0x000000FF);

			PC = ((highByte << 8) | loByte) & 0x0000FFFF;

		} else {
			int hiByte = ((PC & 0xFF00) >> 8);
			int loByte = (PC & 0xFF);

			bus.write(STACK_POINTER, (byte) hiByte);
			STACK_POINTER++;
			bus.write(STACK_POINTER, (byte) loByte);
			STACK_POINTER++;

			PC = inst & 0x0FFF;
		}
	}

	// Display something to our screen.
	// PASSED
	public void display(short inst) {
		// Get the height of our sprite.
		int height = inst & 0x000F;
		// retrieve our x,y cordinates for our sprite.
		int x = V_REGISTERS[(inst & 0x0F00) >> 8] & 0xFF;
		int y = V_REGISTERS[(inst & 0x00F0) >> 4] & 0xFF;
		V_REGISTERS[0xF] = 0;

		for (int i = 0; i < height; i++) {
//			if (y + i >= 32)
//				break;
			byte spriteData = bus.read(I_REGISTER + i);
			byte mask = (byte) 0x80;
			// loop through each pixel 1 at a time.
			for (int j = 0; j < 8; j++) {
//				if (x + j >= 64)
//					break;
				int set = spriteData & mask;
				spriteData <<= 1;

				if (bus.display[(x + j) % 64][(y + i) % 32] && set != 0) {
					V_REGISTERS[0xF] = 1;
				}
				bus.display[(x + j) % 64][(y + i) % 32] = (bus.display[(x + j) % 64][(y + i) % 32] ^ set != 0);

			}
		}
		drawScreen = true;
	}

	// Our chip comes pre-installed with fonts. Conventions have us putting these
	// fonts at 0x050-0x09F.
	// This font was found on the tobiasvl.github.io page.
	public void initializeChip(Bus bus) {
		this.bus = bus;

		for (int i = 0; i < 16; i++)
			V_REGISTERS[i] = 0;

		// 0
		bus.write(50, (byte) 0xF0);
		bus.write(51, (byte) 0x90);
		bus.write(52, (byte) 0x90);
		bus.write(53, (byte) 0x90);
		bus.write(54, (byte) 0xF0);

		// 1
		bus.write(55, (byte) 0x20);
		bus.write(56, (byte) 0x60);
		bus.write(57, (byte) 0x20);
		bus.write(58, (byte) 0x20);
		bus.write(59, (byte) 0x70);

		// 2
		bus.write(60, (byte) 0xF0);
		bus.write(61, (byte) 0x10);
		bus.write(62, (byte) 0xF0);
		bus.write(63, (byte) 0x80);
		bus.write(64, (byte) 0xF0);

		// 3
		bus.write(65, (byte) 0xF0);
		bus.write(66, (byte) 0x10);
		bus.write(67, (byte) 0xF0);
		bus.write(68, (byte) 0x10);
		bus.write(69, (byte) 0xF0);

		// 4
		bus.write(70, (byte) 0x90);
		bus.write(71, (byte) 0x90);
		bus.write(72, (byte) 0xF0);
		bus.write(73, (byte) 0x10);
		bus.write(74, (byte) 0x10);

		// 5
		bus.write(75, (byte) 0xF0);
		bus.write(76, (byte) 0x80);
		bus.write(77, (byte) 0xF0);
		bus.write(78, (byte) 0x10);
		bus.write(79, (byte) 0xF0);

		// 6
		bus.write(80, (byte) 0xF0);
		bus.write(81, (byte) 0x80);
		bus.write(82, (byte) 0xF0);
		bus.write(83, (byte) 0x90);
		bus.write(84, (byte) 0xF0);

		// 7
		bus.write(85, (byte) 0xF0);
		bus.write(86, (byte) 0x10);
		bus.write(87, (byte) 0x20);
		bus.write(88, (byte) 0x40);
		bus.write(89, (byte) 0x40);

		// 8
		bus.write(90, (byte) 0xF0);
		bus.write(91, (byte) 0x90);
		bus.write(92, (byte) 0xF0);
		bus.write(93, (byte) 0x90);
		bus.write(94, (byte) 0xF0);

		// 9
		bus.write(95, (byte) 0xF0);
		bus.write(96, (byte) 0x90);
		bus.write(97, (byte) 0xF0);
		bus.write(98, (byte) 0x10);
		bus.write(99, (byte) 0xF0);

		// A
		bus.write(100, (byte) 0xF0);
		bus.write(101, (byte) 0x90);
		bus.write(102, (byte) 0xF0);
		bus.write(103, (byte) 0x90);
		bus.write(104, (byte) 0x90);

		// B
		bus.write(105, (byte) 0xE0);
		bus.write(106, (byte) 0x90);
		bus.write(107, (byte) 0xE0);
		bus.write(108, (byte) 0x90);
		bus.write(109, (byte) 0xE0);

		// C
		bus.write(110, (byte) 0xF0);
		bus.write(111, (byte) 0x80);
		bus.write(112, (byte) 0x80);
		bus.write(113, (byte) 0x80);
		bus.write(114, (byte) 0xF0);

		// D
		bus.write(115, (byte) 0xE0);
		bus.write(116, (byte) 0x90);
		bus.write(117, (byte) 0x90);
		bus.write(118, (byte) 0x90);
		bus.write(119, (byte) 0xE0);

		// E
		bus.write(120, (byte) 0xF0);
		bus.write(121, (byte) 0x80);
		bus.write(122, (byte) 0xF0);
		bus.write(123, (byte) 0x80);
		bus.write(124, (byte) 0xF0);

		// F
		bus.write(125, (byte) 0xF0);
		bus.write(126, (byte) 0x80);
		bus.write(127, (byte) 0xF0);
		bus.write(128, (byte) 0x80);
		bus.write(129, (byte) 0x80);

	}

	public void newFrame() {
		DELAY_TIMER--;
		SOUND_TIMER--;

		if (DELAY_TIMER < 0) {
			DELAY_TIMER = 0;
		}

		if (SOUND_TIMER < 0) {
			SOUND_TIMER = 0;
		}
	}

}
