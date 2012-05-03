;detect monitor
   HWN Z

:next_hw
   IFE Z,0
       SET PC,end_hw
   SUB Z,1
   HWQ Z
   IFE B,0x7349
       IFE A,0xf615
           SET [mon_hw],Z
   SET PC,next_hw

; configure monitor: screenbuffer at 0x8000
:end_hw
   SET A,0
   SET B,0x8000
   HWI [mon_hw]
   SET I,message
   SET J,0x7fff			; J is incremented before writing
   SET Z,0xf000

:loop
   STI A,[I]
   BOR A,Z
   SET [J],A
   IFE [I],0			; end of msg - switch colors and start over
       ADD Z,0x0100		; switch colors
   IFE [I],0
       SET I,message
   IFE J,0x817f			; end of vram - start over
       SET J,0x7fff
   SET PC,loop
   HCF 0

:message
   dat "Hello, World!!!!",0

:mon_hw dat 0
