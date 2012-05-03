;1 Assembler test for DCPU
;2 by Markus Persson

             set a, 0xbeef                        ;4 Assign 0xbeef to register a
             set [0x1000], a                      ;5 Assign memory at 0x1000 to value of register a
             ifn a, [0x1000]                      ;6 Compare value of register a to memory at 0x1000 ..
                 set PC, end                      ;7 .. and jump to end if they don't match

             set i, 0                             ;9 Init loop counter, for clarity
:nextchar    ife [data+i], 0                      ;10 If the character is 0 ..
                 set PC, end                      ;11 .. jump to the end
             set [0x8000+i], [data+i]             ;12 Video ram starts at 0x8000, copy char there
             add i, 1                             ;13 Increase loop counter
             set PC, nextchar                     ;14 Loop

:data        dat "Hello_world!", 0                ;16 Zero terminated string

:end         HCF 0
