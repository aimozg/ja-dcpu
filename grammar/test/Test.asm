SET A, 0
:start
set b,0x8000
jsr 0x1000
jsr start
jsr end
SET C,0
:end
add a,1
