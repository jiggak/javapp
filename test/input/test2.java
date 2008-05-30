//#if defined(pee)
System.out.println("pee is defined");

//#if ${pee} == "pee"
System.out.println("pee is \"pee\"");
//#elif ${pee} == "poo"
System.out.println("pee is \"poo\"");
//#endif

//#else
System.out.println("pee is undefined");
//#endif
