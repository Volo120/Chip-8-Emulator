# CHIP-8-Emulator
This is a CHIP-8 emulator, providing a graphical interface to run classic CHIP-8 ROMs. It includes a basic debugger to help understand the emulator's state during execution.

## Features

* **CHIP-8 Instruction Set Emulation**: Accurately emulates the core CHIP-8 instruction set.
* **Graphical Display**: Renders the 64x32 monochrome display of the CHIP-8.
* **Keyboard Input**: Maps standard PC keys to the CHIP-8 hexadecimal keypad.
* **Sound Emulation**: Plays a "beep" sound when the CHIP-8 sound timer is active.
* **ROM Loading**: Allows loading CHIP-8 ROM files through a file chooser dialog.
* **Debugger Tool**: A separate window displaying:
    * Memory content
    * Registers (V0-VF, I)
    * Program Counter (PC)
    * Current Instruction
    * Call Stack
* **Emulation Control**: Pause, resume, and step through instructions (only when paused).
* **Reset/Restart**: Options to reset the emulator or restart the currently loaded ROM.

## Screenshots
![UI](https://files.catbox.moe/ca9vcq.PNG "UI")

---

![Debugger](https://files.catbox.moe/14f74b.PNG "Debugger")

## How to Run
Download the latest release from the [releases](https://github.com/Volo120/CHIP-8-Emulator/releases) page

Unzip and run it by executing this command:

```bash
java -jar C8-Emu.jar
```
