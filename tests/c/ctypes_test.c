        #include <stdlib.h>
        #include <stdio.h>

        struct x
        {
           int x1;
           short x2;
           char x3;
           struct x* next;
        };

        void nothing()
        {
        }

        char inner_struct_elem(struct x *x1)
        {
           return x1->next->x3;
        }

        struct x* create_double_struct()
        {
           struct x* x1, *x2;

           x1 = (struct x*)malloc(sizeof(struct x));
           x2 = (struct x*)malloc(sizeof(struct x));
           x1->next = x2;
           x2->x2 = 3;
           return x1;
        }

        void free_double_struct(struct x* x1)
        {
            free(x1->next);
            free(x1);
        }
        
        const char *static_str = "xxxxxx";
        const long static_int = 42;
        const double static_double = 42.42;
        
        unsigned short add_shorts(short one, short two)
        {
           return one + two;
        }

        void* get_raw_pointer()
        {
           return (void*)add_shorts;
        }

        char get_char(char* s, unsigned short num)
        {
           return s[num];
        }

        char *char_check(char x, char y)
        {
           if (y == static_str[0])
              return static_str;
           return NULL;
        }

        int get_array_elem(int* stuff, int num)
        {
           return stuff[num];
        }

        struct x* get_array_elem_s(struct x** array, int num)
        {
           return array[num];
        }

        long long some_huge_value()
        {
           return 1LL<<42;
        }

        unsigned long long some_huge_uvalue()
        {
           return 1LL<<42;
        }

        long long pass_ll(long long x)
        {
           return x;
        }

        static int prebuilt_array1[] = {3};

        int* allocate_array()
        {
            return prebuilt_array1;
        }

        long long runcallback(long long(*callback)())
        {
            return callback();
        }

        struct x_y {
            long x;
            long y;
        };

        long sum_x_y(struct x_y s) {
            return s.x + s.y;
        }

        struct s2h {
            short x;
            short y;
        };

        struct s2h give(short x, short y) {
            struct s2h out;
            out.x = x;
            out.y = y;
            return out;
        }

        struct s2h perturb(struct s2h inp) {
            inp.x *= 2;
            inp.y *= 3;
            return inp;
        }
