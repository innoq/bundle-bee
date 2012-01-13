package org.bundlebee.testbundle.cli;

import org.eclipse.osgi.framework.console.CommandProvider;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.bundlebee.testbundle.TestBundle;
import org.bundlebee.testbundle.impl.TestBundleReturn;

/**
 * TestCommand.
 * @author joergp
 */
public class TestCommand implements CommandProvider {

   private final TestBundle mTestBundle;

    public TestCommand(TestBundle b) {
        mTestBundle = b;
    }


    public String getHelp() {
        return "\tbbtest <message> - calls a couple of services\n";
    }

    public void _bbtest(final CommandInterpreter commandInterpreter) {
		final StringBuilder sb = new StringBuilder();
		String arg;
		while ((arg = commandInterpreter.nextArgument()) != null) {
			sb.append(arg);
			sb.append(' ');
		}

		mTestBundle.beep();
		System.out.println( "called beep: void" );

		TestBundleReturn ret = mTestBundle.echo(sb.toString().trim());
		System.out.println( "called echo( \"" + sb.toString().trim() + "\" ): " + ret.getMessage() );

		ret = mTestBundle.echoReturn(ret);
		System.out.println( "called echoReturn( \"" + sb.toString().trim() + "\" ): " + ret.getMessage() );

		int i = mTestBundle.returnIntPrimitive( 27 );
		System.out.println("called returnIntPrimitive(): " + i );

		short s = mTestBundle.returnShortPrimitive( (short)27 );
		System.out.println("called returnShortPrimitive(): " + i );

		long l = mTestBundle.returnLongPrimitive( 123456788987654321L );
		System.out.println("called returnLongPrimitive(): " + l );

		float f = mTestBundle.returnFloatPrimitive( 1.234f );
		System.out.println("called returnFloatPrimitive(): " + f );

		double d = mTestBundle.returnDoublePrimitive( 9.87654321 );
		System.out.println("called returnFloatPrimitive(): " + d );

		boolean b = mTestBundle.returnBooleanPrimitive( true );
		System.out.println("called returnBooleanPrimitive(): " + b );

		byte by = mTestBundle.returnBytePrimitive( (byte)19 );
		System.out.println("called returnBytePrimitive(): " + by );

		char c = mTestBundle.returnCharPrimitive( 'x' );
		System.out.println("called returnCharPrimitive(): " + c );

		try {
			mTestBundle.throwException();
		} catch (Exception ex) {
			System.out.println("called throwException(): " + ex );
		}
    }

}
