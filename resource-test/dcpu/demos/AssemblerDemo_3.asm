; Reading characters from the keyboard
; by Markus Persson

#macro push(x){
	set push,x
}
#macro pop(x)
	set x,pop

#macro nextkey(target) {
	push(i)
	set i,[keypointer]
	add i,0x9000
	set target,[i]
	ife target,0
		jmp end

	set [i],0
	add [keypointer], 1
	and [keypointer], 0xf
:end
	pop(i)
}
nextkey(4)

:keypointer
dat 0
