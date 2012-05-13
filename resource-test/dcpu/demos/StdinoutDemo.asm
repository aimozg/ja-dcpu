:main
    ; locate stdio
    HWN Z
:nexthw
    IFE Z,0
        SET PC,endhw
    SUB Z,1
    HWQ Z
    IFE B,0x494f ;'IO'
        IFE A,0x5344 ;'SD'
            SET [stdio_hw],Z
    SET PC,nexthw

:endhw
    ; println(greet1)
    SET B,greet1
    JSR println
    ; readln(name)
    SET I,name
    JSR readln
    ; append zero to name
    SET [I],0
    ; println(greet2)
    SET B,greet2
    JSR println
    ; println(name)
    SET B,name
    JSR println
    ; println(greet3)
    SET B,greet3
    JSR println
    ; goto end
    SET PC,end

; println()
;        prints zero-terminated line to output
; Input:
;       [B],[B+1],... - zero-terminated line
:println
    SET PUSH,A
    SET A,2
    HWI [stdio_hw]
    SET A,POP
    SET PC,POP

; readln()
;      reads \n-terminated line from input (without \n)
; Input:
;       I - pointer to input buffer
; Output:
;       I - pointer beyond last character of input

:readln
    SET PUSH,A
    SET PUSH,J
    SET A,1
    :readln_getc
        HWI [stdio_hw]
        IFE B,0xffff ; if nothing on input yet, repeat
            SET PC,readln_getc
        IFE B,0x0a
            SET PC,readln_end
        STI [I],B
        SET PC,readln_getc
:readln_end
    SET J,POP
    SET A,POP
    SET PC,POP

:stdio_hw
    dat 0
:greet1
    dat "Hello, what's your name?",0xd,0xa,0
:greet2
    dat "Hello, ",0
:greet3
    dat "!",0xd,0xa,0
:end
    hcf 0
:name
