# JA-DCPU

Toolchain for DCPU-16 processor used in 0x10c game (http://0x10c.com/).

Written in Java.

As of 06.04.2012, contains:

* Emulator
 * Can trace execution (prints instructions, register, stack, memory)
* Assembler
 * Can print maps: label to value, source line to address, code/data layout
* Disassembler
* Peripherals framework
 * Peripherals can intercept reads/writes to specific memory regions

## Implementation specifics

1. Reserved instructions halt the cpu.
2. "Register value" as operand means value *before* instruction execution. If instruction modifies PC/SP, their unmodified values are used (so SET PUSH, SP saves stack position before PUSH; SET [0xbeef], PC saves this instruction address).

## Usage

To play with DCPU, take ja-dcpu-demo.jar from Downloads section, and run it from console

> java -jar ja-dcpu-demo.jar

### Assembly and emulation

Pass source filename as an argument. It will be assembled and executed.

> java -jar ja-dcpu-demo.jar __yoursourcefile__

### Binary files

You can save assembled binary image by -O **binaryoutput** parameter, and load it with -I **binaryinput** parameter. (-I is capital i, not lower L)

> java -jar ja-dcpu-demo.jar __yoursourcefile__ -O __yourbinary__

> java -jar ja-dcpu-demo.jar -I __yourbinary__

### Disassembly

> java -jar ja-dcpu-demo.jar -I __binary__ -D __disassembled__

### Tracing

You can enable tracing with -T and flags. Flags are:

* r --- trace registers
* m --- trace memory where registers point
* s --- trace stack (8 words deep)

#### Example output:

    0000: SET A, 8000
      R:  A=0000 B=0000 C=0000 X=0000 Y=0000 Z=0000 I=0000 J=0000  SP=ffff O=0000
      M:  A*7c01 B*7c01 C*7c01 X*7c01 Y*7c01 Z*7c01 I*7c01 J*7c01  SP*0000 O*7c01
      S:  0000 0000 0000 0000 0000 0000 0000 0000
    0002: SET B, 9000
      R:  A=8000 B=0000 C=0000 X=0000 Y=0000 Z=0000 I=0000 J=0000  SP=ffff O=0000
      M:  A*0000 B*7c01 C*7c01 X*7c01 Y*7c01 Z*7c01 I*7c01 J*7c01  SP*0000 O*7c01
      S:  0000 0000 0000 0000 0000 0000 0000 0000
    0004: SET I, 002b
      R:  A=8000 B=9000 C=0000 X=0000 Y=0000 Z=0000 I=0000 J=0000  SP=ffff O=0000
      M:  A*0000 B*0000 C*7c01 X*7c01 Y*7c01 Z*7c01 I*7c01 J*7c01  SP*0000 O*7c01
      S:  0000 0000 0000 0000 0000 0000 0000 0000
    0006: JSR 001b
      R:  A=8000 B=9000 C=0000 X=0000 Y=0000 Z=0000 I=002b J=0000  SP=ffff O=0000
      M:  A*0000 B*0000 C*7c01 X*7c01 Y*7c01 Z*7c01 I*0048 J*7c01  SP*0000 O*7c01
      S:  0000 0000 0000 0000 0000 0000 0000 0000

First line is **address**: **instruction**

Second line contains register values.

Third line contains [A], [B] etc.

Fourth line contains [SP], [SP+1], [SP+2], and so on.

All values are given _before_ instruction evaluation;

### Mapping

If you assemble source, you can create map-file with information about labels, source lines, and code/data sectors.

Example map file:

	;;;;MAPSTART
	;;;;SYMBOLS
	; Format: "label"=address
	;; "end"=0x0052
	...
	;; "println"=0x001b
	;; "readln"=0x0021
	;;;;SRCMAP
	; Format: source line=address
	;; 2=0x0000
	;; 3=0x0001
	;; 5=0x0003
	;; 6=0x0005
	...
	;; 60=0x0045
	;; 61=0x0046
	;; 62=0x004d
	;; 63=0x004e
	;; 64=0x0051
	;; 66=0x0052
	;;;;CODE
	; Format: code_start-code_end
	;; code 0x0000-0x002a
	;;;;MAPEND


### Flags

* -x prevents code from execution. By default, even if you specify -O or -D flags, code will be executed. If you want just create or disassemble binary file, add -x.

### Peripherals

Framework supports flexible peripherals. ja-dcpu-demo uses two hard-coded peripherals:

1. 0x8000-0x8fff Stdout. Anything written comes to stdout
2. 0x9000-0x9fff Stdin. Reading returns character typed, or 0xffff if no input yet (this-is a non-blocking operation).

## Example code

See downloads section

## TO-DO

1. Utilize mapfiles in tracing
1. Configurable peripherals
2. More peripherals
  1. Network access
  2. Display
  3. Sound
3. IDE
  1. Syntax highlighting
  2. Debugger:
    1. Step
    2. Show source
    3. Monitor/modify registers and memory
    4. Breakpoints
  3. Peripheral constructor