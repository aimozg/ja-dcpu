						;   LONGS		 SHORTS		 RELOC
						; =========		========	========

	SET A, 0			; 8401			8401		8401
:loop
	ADD A, 1			; 8802			8802		8802
	IFE A, 5			; 9812			9812		9812
		SET PC, out		; 7f81 0007		9b81		8b82	= ADD PC, 1 <-- tricky, needs to know FINAL out position to calculate diff
	SET PC, loop		; 7f81 0001		8b81		9783	= SUB PC, 4
:out
	SET PC, out			; 7f81 0007		9b81		8b83	= SUB PC, 1
