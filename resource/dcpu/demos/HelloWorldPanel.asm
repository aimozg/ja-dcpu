            set a, 1
            add a, 1
            ife a, 2
                set a, 3
:mainloop
            ife [message + I], 0
                set pc, end
            set a, [message + I]
            add a, 0xA100
            set [0x8000 + I], a
            add i, 1
            set pc, mainloop
:message    dat "Hello, world!", 0
:end        set pc, end
