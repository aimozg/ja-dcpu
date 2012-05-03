; initialize monitor, keyboard, clock
	HWN A
	SET Z, A

:device_set_loop
	SUB Z, 1
	HWQ Z
	IFE A, 0xf615
		JSR monitor_setup
	IFE A, 0x7406
		JSR keyboard_setup
	IFE A, 0xb402
		JSR clock_setup
	IFN Z, 0
  		SET PC, device_set_loop

	; initialize console
	SET C, 0
	SET I, 0x8020
	SET [I], [cursor]
	SET B, I
	SUB B, 1
	SET X, I
	ADD X, 0x15f

; wait for input
:console_loop
	SET A, 1
	HWI [keyboard_address]
	IFE C, 0		;; no input
  		SET PC, console_loop
	IFE C, 8		;; for some reason after going through backspace an HWI call will return 8?
  		SET PC, console_loop

	IFE C, 0x11		;; enter pressed
  		SET PC, console_end

	IFE C, 0x10		;; backspace pressed
  		JSR backspace

	; echo input, move to next character
	BOR C, 0x2000
	IFN C, 0x2010
  		SET [I], C
	ADD I, 1
	IFG I, X
	  SUB I, 1
	SET C, 0
	SET [I], [cursor]
	SET PC, console_loop

:backspace
	SET [I], 0x0
	SUB I, 1
	IFN I, B
  		SET [I], 0x0
	IFN I, B
  		SUB I, 1
	SET PC, POP

:console_end
	SET [I], 0x0
	;; remove cursor
	SET PC, console_end

:monitor_setup
	SET [monitor_address],Z

	;map screen to ram
	SET A, 0
	SET B, 0x8000
	HWI Z

;initialize clock
:monitor_setup_clock
	SET [B], 0xf030
	ADD B, 1
	IFN B, 0x8005
  		SET PC, monitor_setup_clock
	SET [0x8002], 0xf03A

	;set background color
	SET A, 3
	SET B, 0x0
	HWI Z
	SET PC, POP

:keyboard_setup
	SET [keyboard_address], Z

	;clear keyboard buffer
	SET A, 0
	HWI Z
	SET PC, POP

:clock_setup
	SET [clock_address], Z

	;tick once every second
	SET A, 0
	SET B, 60
	HWI [clock_address]

	;interrupt at every tick
	IAS update_clock
	SET A, 2
	SET B, 0x8004
	HWI [clock_address]
	SET PC, POP

;adds a second to the clock for every tick
:update_clock
	IFE A, 0x8003
  	IFE [A], 0xf035
  		ADD PC, 3
	IFN [A], 0xf039
  		SET PC, clock_add
	SET [A], 0xf030
	SUB A, 1
	IFE A, 0x8002
  		SUB A, 1
	IFN A, 0x7fff
		SET PC, update_clock
	SET PC, end_clock_interrupt

:clock_add
	ADD [A], 1

:end_clock_interrupt
	RFI 0

:cursor
	DAT 0x209c

:keyboard_address
	DAT 0

:monitor_address
	DAT 0

:clock_address
	DAT 0
