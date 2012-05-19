:start
add b,1
add b,2
add b,3
add b,4
add b,5
add b,6
add b,7
add b,8
add b,9
add b,10
add b,11
add b,12
add b,13
add b,14
add b,15
add b,16
add b,17
add b,18
add b,19
add b,20
add b,21
add b,22
add b,23
add b,24
add b,25
add b,26
add b,27
add b,28
add b,29
add b,30
add b,31

:shortback

add b,32

jsr 0x1000			; long jump
jsr start   		; jump to reference in 0-30 region can be short
jsr shortback		; short back step away, but in memory > 30 so 2 words. not relocatable without completely changing to using a memory address to hold value
set pc, start		; setting to a ref in 0-30 region can be short
set pc, shortforward	; the reference is >30 memory address, but we could optimize it to "add pc, <short>"
jsr shortforward  	; short forward reference, but in memory > 30, so is 2 word instruction - again, not relocatable without effort
jsr end     		; long forward reference, same as previous
jsr 1				; short jump

add a,1
add a,2
add a,3
add a,4
add a,5
add a,6
add a,7
add a,8
add a,9
add a,10
add a,11
add a,12
add a,13
add a,14

:shortforward

add a,15
add a,16
add a,17
add a,18
add a,19
add a,20
add a,21
add a,22
add a,23
add a,24
add a,25
add a,26
add a,27
add a,28
add a,29
add a,30
add a,31
add a,32

:end
set a, 1
