package sorcer.core.invoker;

import net.jini.core.transaction.TransactionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.tester.provider.impl.AdderImpl;
import sorcer.arithmetic.tester.provider.impl.MultiplierImpl;
import sorcer.arithmetic.tester.provider.impl.SubtractorImpl;
import sorcer.arithmetic.tester.volume.Volume;
import sorcer.core.context.model.par.Par;
import sorcer.core.context.model.par.ParModel;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.service.*;
import sorcer.service.modeling.Model;
import sorcer.util.Sorcer;
import sorcer.util.exec.ExecUtils.CmdResult;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.rmi.RemoteException;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.args;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.pipe;
import static sorcer.eo.operator.value;
import static sorcer.po.operator.add;
import static sorcer.po.operator.alt;
import static sorcer.po.operator.*;
import static sorcer.po.operator.get;
import static sorcer.po.operator.loop;
import static sorcer.po.operator.map;
import static sorcer.po.operator.opt;
import static sorcer.po.operator.put;
import static sorcer.po.operator.set;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("core/sorcer-int-tests/sorcer-tester")
public class InvokerTest {
	private final static Logger logger = LoggerFactory.getLogger(InvokerTest.class);

	private ParModel pm; 
	private Par<Double> x;
	private Par<Double> y;
	private Par z;
		
	@Before
	public void initParModel() throws EvaluationException, RemoteException {
		pm = new ParModel();
		x = par("x", 10.0);
		y = par("y", 20.0);
		z = par("z", invoker("x - y", x, y));

	}

	// member subclass of Invocable with Context parameter used below with
	// contextMethodAttachmentWithArgs()
	// there are constructor's context and invoke metod's context as parameters
	public class Update extends Invocable {
		public Update(Context context) {
			super(context);
		}

		public Double invoke(Context arg) throws Exception {
			x.setValue(arg.getValue("x"));
			y.setValue(context.getValue("y"));
			// x set from 'arg'
			assertTrue(value(x).equals(200.0));
			// y set from construtor's context 'in'
			assertTrue(value(y).equals(30.0));
			assertTrue(value(z).equals(170.0));
			return value(x) + value(y) + (double) value(pm, "z");
		}
	};

	@Test
	public void lambdaInvoker() throws RemoteException, ContextException,
			SignatureException, ExertionException {
		Invocation invoker = invoker("lambda",
				cxt ->  (double) value(cxt, "x") + (double) value(cxt, "y") + 30,
				context(ent("x", 10.0), ent("y", 20.0)));
		logger.info("invoke value: " + invoke(invoker));
		assertEquals(invoke(invoker), 60.0);
	}

	@Test
	public void methodInvokerTest() throws RemoteException, ContextException {
		set(x, 10.0);
		set(y, 20.0);
		add(pm, x, y, z);

		Context in = context(ent("x", 20.0), ent("y", 30.0));
		Context arg = context(ent("x", 200.0), ent("y", 300.0));
		add(pm, methodInvoker("invoke", new Update(in), arg));
		logger.info("call value:" + invoke(pm, "invoke"));
		assertEquals(invoke(pm, "invoke"), 400.0);
	}

	@Test
	public void groovyInvokerTest() throws RemoteException, ContextException,
			SignatureException, ExertionException {
		ParModel pm = parModel("par-model");
		add(pm, par("x", 10.0), par("y", 20.0));
		add(pm, invoker("expr", "x + y + 30", args("x", "y")));
		logger.info("invoke value: " + invoke(pm, "expr"));
		assertEquals(invoke(pm, "expr"), 60.0);
		logger.info("get value: " + value(pm, "expr"));
		assertEquals(value(pm, "expr"), 60.0);
	}

	@Test
	public void lambdaInvokerTest() throws RemoteException, ContextException,
			SignatureException, ExertionException {
		ParModel pm = parModel("model");
		add(pm, par("x", 10.0), par("y", 20.0));
		add(pm, invoker("lambda", cxt -> (double)value(cxt, "x") + (double)value(cxt, "y") + 30));
		logger.info("invoke value: " + invoke(pm, "lambda"));
		assertEquals(invoke(pm, "lambda"), 60.0);
		logger.info("get value: " + value(pm, "lambda"));
		assertEquals(value(pm, "lambda"), 60.0);
	}

	@Test
	public void lambdaInvokerTest2() throws Exception {

		Model mo = model(ent("x", 10.0), ent("y", 20.0),
				ent(invoker("lambda", cxt -> (double) value(cxt, "x")
									+ (double) value(cxt, "y")
									+ 30)));
		logger.info("invoke value: " + value(mo, "lambda"));
		assertEquals(value(mo, "lambda"), 60.0);
	}

	@Test
	public void lambdaInvokerTest3() throws Exception {


		Context scope = context(ent("x1", 20.0), ent("y1", 40.0));

		Model mo = model(ent("x", 10.0), ent("y", 20.0),
			ent(invoker("lambda", (cxt) -> {
						return (double) value(cxt, "x")
								+ (double) value(cxt, "y")
								+ (double) value(cxt, "y1")
								+ 30;
					},
				scope)));
		logger.info("invoke value: " + value(mo, "lambda"));
		assertEquals(value(mo, "lambda"), 100.0);
	}

	@Test
	public void invokeTaskTest() throws RemoteException, ContextException,
			SignatureException, ExertionException {

		Task t4 = task(
				"t4",
				sig("multiply", MultiplierImpl.class),
				context("multiply", inEnt("arg/x1", 50.0), inEnt("arg/x2", 10.0),
						result("result/y")));

		// logger.info("invoke value:" + invoke(t4));
		assertEquals(invoke(t4), 500.0);
	}

	@Test
	public void invokeJobTest() throws RemoteException, ContextException,
			SignatureException, ExertionException, TransactionException {
		Context c4 = context("multiply", inEnt("arg/x1", 50.0),
				inEnt("arg/x2", 10.0), result("result/y"));
		Context c5 = context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
				result("result/y"));

		// exertions
		Task t3 = task(
				"t3",
				sig("subtract", SubtractorImpl.class),
				context("subtract", inEnt("arg/x1"), inEnt("arg/x2"), outEnt("result/y")));
		Task t4 = task("t4", sig("multiply", MultiplierImpl.class), c4);
		Task t5 = task("t5", sig("add", AdderImpl.class), c5);

		Job j1 = job("j1", sig("exert", ServiceJobber.class),
					job("j2", t4, t5, sig("exert", ServiceJobber.class)), t3,
					pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
					pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")),
					result("j1/t3/result/y"));

		// logger.info("invoke value:" + invoke(j1));
		assertEquals(invoke(j1), 400.0);
	}

	@Test
	public void invokeParJobTest() throws RemoteException, ContextException,
			SignatureException, ExertionException {
		Context c4 = context("multiply", inEnt("arg/x1"), inEnt("arg/x2"),
				result("result/y"));
		Context c5 = context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
				result("result/y"));

		// exertions
		Task t3 = task(
				"t3",
				sig("subtract", SubtractorImpl.class),
				context("subtract", inEnt("arg/x1"), inEnt("arg/x2"), outEnt("result/y")));
		Task t4 = task("t4", sig("multiply", MultiplierImpl.class), c4);
		Task t5 = task("t5", sig("add", AdderImpl.class), c5);

		Job j1 = job("j1", sig("exert", ServiceJobber.class),
					job("j2", t4, t5, sig("exert", ServiceJobber.class)), t3,
					pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
					pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")),
					result("j1/t3/result/y"));

		// logger.info("return path:" + j1.getReturnJobPath());
		assertEquals(j1.getReturnPath().path, "j1/t3/result/y");

		ParModel pm = parModel("par-model");
		add(pm, map(par("x1p", "arg/x1"), c4), map(par("x2p", "arg/x2"), c4), j1);
		// setting context parameters in a job
		set(pm, "x1p", 10.0);
		set(pm, "x2p", 50.0);

		add(pm, j1);
		// logger.info("call value:" + invoke(pm, "j1"));
		assertEquals(invoke(pm, "j1"), 400.0);
	}

	@Test
	public void invokeParTest() throws RemoteException, ContextException,
			SignatureException, ExertionException {

		Par<Double> x1 = par("x1", 1.0);
		// logger.info("invoke value:" + invoke(x1));
		assertEquals(invoke(x1), 1.0);
	}

	@Test
	public void invokeParArgTest() throws RemoteException, ContextException,
			SignatureException, ExertionException {
		Par x1, x2, y;
		x1 = par("x1", 1.0);
		x2 = par("x2", 2.0);
		y = par("y", invoker("x1 + x2", args("x1", "x2")));
		
//		logger.info("y: " + value(y));
//		assertEquals(value(y), 3.0);

		invoke(y, ent("x1", 10.0), ent("x2", 20.0));
//		logger.info("y: " + value(y));
		assertTrue(value(y).equals(30.0));

//		logger.info("y scope: " + scope(y));
		assertEquals(scope(y), null);
	}

	@Test
	public void exertionInvokerTest() throws RemoteException, ContextException,
			SignatureException, ExertionException {
		Context c4 = context("multiply", inEnt("arg/x1"), inEnt("arg/x2"),
				result("result/y"));
		Context c5 = context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
				result("result/y"));

		// exertions
		Task t3 = task(
				"t3",
				sig("subtract", SubtractorImpl.class),
				context("subtract", inEnt("arg/x1"), inEnt("arg/x2"), outEnt("result/y")));
		Task t4 = task("t4", sig("multiply", MultiplierImpl.class), c4);
		Task t5 = task("t5", sig("add", AdderImpl.class), c5);

		Job j1 = job("j1", sig("exert", ServiceJobber.class),
				job("j2", t4, t5, sig("exert", ServiceJobber.class)), t3,
				pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));

		ParModel pm = parModel("par-model");
		add(pm, map(par("x1p", "arg/x1"), c4), map(par("x2p", "arg/x2"), c4), j1);
		// setting context parameters in a job
		set(pm, "x1p", 10.0);
		set(pm, "x2p", 50.0);

		add(pm, exertInvoker("invoke j1", j1, "j1/t3/result/y"));
		// logger.info("call value:" + invoke(pm, "invoke j1"));
		assertEquals(invoke(pm, "invoke j1"), 400.0);
	}

	@Test
	public void cmdInvokerTest() throws SignatureException, ExertionException,
			ContextException, IOException {
		String riverVersion = System.getProperty("river.version");
		String sorcerVersion = System.getProperty("sorcer.version");
		String slf4jVersion = System.getProperty("slf4j.version");
		String logbackVersion = System.getProperty("logback.version");
		String buildDir = System.getProperty("project.build.dir");

		String cp = buildDir + "/libs/sorcer-tester-" + sorcerVersion + ".jar" + File.pathSeparator
				+ Sorcer.getHome() + "/lib/sorcer/lib/sorcer-platform-" + sorcerVersion + ".jar"  + File.pathSeparator
				+ Sorcer.getHome() + "/lib/logging/slf4j-api-" + slf4jVersion + ".jar"  + File.pathSeparator
				+ Sorcer.getHome() + "/lib/logging/logback-core-" + logbackVersion + ".jar"  + File.pathSeparator
				+ Sorcer.getHome() + "/lib/logging/logback-classic-" + logbackVersion + ".jar"  + File.pathSeparator
				+ Sorcer.getHome() + "/lib/river/jsk-platform-" + riverVersion + ".jar"  + File.pathSeparator
				+ Sorcer.getHome() + "/lib/river/jsk-lib-" + riverVersion + ".jar ";


		ServiceInvoker cmd = cmdInvoker("volume",
				"java -cp  " + cp + Volume.class.getName() + " cylinder");

		ParModel pm = parModel(par(cmd),
				par("x", 10.0), par("y"),
				par("multiply", invoker("x * y", pars("x", "y"))),
				par("add", invoker("x + y", pars("x", "y"))));

		CmdResult result = (CmdResult) invoke(pm, "volume");
		// get from the result the volume of cylinder and assign to y parameter
		assertTrue("EXPECTED '0' return value, GOT: "+result.getExitValue(), result.getExitValue() == 0);
		Properties props = new Properties();
		props.load(new StringReader(result.getOut()));

		set(pm, "y", new Double(props.getProperty("cylinder/volume")));

		logger.info("x value:" + value(pm, "x"));
		logger.info("y value:" + value(pm, "y"));
		logger.info("multiply value:" + value(pm, "add"));
		assertEquals(value(pm, "add"), 47.69911184307752);
	}

	@Test
	public void conditionalInvoker() throws RemoteException, ContextException {
		final ParModel pm = new ParModel("par-model");
		pm.putValue("x", 10.0);
		pm.putValue("y", 20.0);
        pm.putValue("condition", invoker("x > y", pars("x", "y")));

		//pm.putValue("condition", new ServiceInvoker(pm));
		
		//((ServiceInvoker) pm.get("condition")).setPars(pars("x", "y")).setEvaluator(
		//		invoker("x > y"));

		
		assertEquals(pm.getValue("x"), 10.0);
		assertEquals(pm.getValue("y"), 20.0);
		
		Object val = pm.getValue("condition");
//		logger.info("condition value: " + val);
//		logger.info("condition value: " + pm.getValue("condition"));
		assertEquals(pm.getValue("condition"), false);

		pm.putValue("x", 300.0);
		pm.putValue("y", 200.0);
		// logger.info("condition value: " + pm.getValue("condition"));
		assertEquals(pm.getValue("condition"), true);

		// enclosing class conditional context
		Condition c = new Condition() {
			@Override
			public boolean isTrue() throws ContextException {
				return (Boolean) pm.getValue("condition");
			}
		};
		assertEquals(c.isTrue(), true);

		// provided conditional context
		Condition eval = new Condition(pm) {
			@Override
			public boolean isTrue() throws ContextException {
				return (Boolean) conditionalContext.getValue("condition");
			}
		};
		assertEquals(eval.getValue(), true);
	}

	@Test
	public void optInvokerTest1() throws RemoteException, ContextException {
		ParModel pm = new ParModel("par-model");

		OptInvoker opt = new OptInvoker("opt", new Condition(pm,
				"{ x, y -> x > y }", "x", "y"), invoker("x + y",
				pars("x", "y")));

		pm.add(opt);
		pm.putValue("x", 10.0);
		pm.putValue("y", 20.0);

		assertEquals(opt.getValue(), null);

		pm.putValue("x", 300.0);
		pm.putValue("y", 200.0);
		logger.info("opt value: " + opt.getValue());
		assertEquals(opt.getValue(), 500.0);
	}

	@Test
	public void optInvokerTest2() throws RemoteException, ContextException {
		// Java 8 lambdas style
		ParModel pm = new ParModel("par-model");

		OptInvoker opt = new OptInvoker("opt", new Condition(pm,
				cxt -> (double)v(cxt, "x") > (double)v(cxt, "y")), invoker("x + y",
				pars("x", "y")));

		pm.add(opt);
		pm.putValue("x", 10.0);
		pm.putValue("y", 20.0);

		assertEquals(opt.getValue(), null);

		pm.putValue("x", 300.0);
		pm.putValue("y", 200.0);
		logger.info("opt value: " + opt.getValue());
		assertEquals(opt.getValue(), 500.0);
	}

	@Test
	public void polOptInvokerTest() throws RemoteException, ContextException {
		ParModel pm = parModel("par-model");
		add(pm,
				par("x", 10.0),
				par("y", 20.0),
				opt("opt", condition(pm, "{ x, y -> x > y }", "x", "y"),
						invoker("x + y", pars("x", "y"))));

		logger.info("opt value: " + value(pm, "opt"));
		assertEquals(value(pm, "opt"), null);

		put(pm, "x", 300.0);
		put(pm, "y", 200.0);
		logger.info("opt value: " + value(pm, "opt"));
		assertEquals(value(pm, "opt"), 500.0);
	}

	@Test
	public void altInvokerTest() throws RemoteException, ContextException {
		ParModel pm = new ParModel("par-model");
		pm.putValue("x", 30.0);
		pm.putValue("y", 20.0);
		pm.putValue("x2", 50.0);
		pm.putValue("y2", 40.0);
		pm.putValue("x3", 70.0);
		pm.putValue("y3", 60.0);

		OptInvoker opt1 = new OptInvoker("opt1", condition(pm,
				"{ x, y -> x > y }", "x", "y"), invoker("x + y + 10",
				pars("x", "y")));

		OptInvoker opt2 = new OptInvoker("opt2", condition(pm,
				"{ x2, y2 -> x2 > y2 }", "x2", "y2"), invoker(
				"x + y + 20", pars("x", "y")));

		OptInvoker opt3 = new OptInvoker("op3", condition(pm,
				"{ x3, y3 -> x3 > y3 }", "x3", "y3"), invoker(
				"x + y + 30", pars("x", "y")));

		// no condition means condition(true)
		OptInvoker opt4 = new OptInvoker("opt4", invoker("x + y + 40",
				pars("x", "y")));

		AltInvoker alt = new AltInvoker("alt", opt1, opt2, opt3, opt4);
		add(pm, opt1, opt2, opt3, opt4, alt);

		logger.info("opt1 value: " + value(opt1));
		assertEquals(value(opt1), 60.0);
		logger.info("opt2 value: " + value(opt2));
		assertEquals(value(opt2), 70.0);
		logger.info("opt3 value: " + value(opt3));
		assertEquals(value(opt3), 80.0);
		logger.info("opt4 value: " + value(opt4));
		assertEquals(value(opt4), 90.0);
		logger.info("alt value: " + value(alt));
		assertEquals(value(alt), 60.0);

		pm.putValue("x", 300.0);
		pm.putValue("y", 200.0);
		logger.info("opt value: " + value(alt));
		assertEquals(value(alt), 510.0);

		pm.putValue("x", 10.0);
		pm.putValue("y", 20.0);
		pm.putValue("x2", 40.0);
		pm.putValue("y2", 50.0);
		pm.putValue("x3", 50.0);
		pm.putValue("y3", 60.0);
		logger.info("opt value: " + alt.invoke());
		assertEquals(value(alt), 70.0);

		pm.putValue("x2", 50.0);
		pm.putValue("y2", 40.0);
		logger.info("opt value: " + alt.invoke());
		assertEquals(value(alt), 50.0);
	}

	@Test
	public void polAltInvokerTest() throws RemoteException, ContextException {
		ParModel pm = parModel("par-model");
		// add(pm, entry("x", 10.0), entry("y", 20.0), par("x2", 50.0),
		// par("y2", 40.0), par("x3", 50.0), par("y3", 60.0));
		add(pm, par("x", 10.0), par("y", 20.0), par("x2", 50.0),
				par("y2", 40.0), par("x3", 50.0), par("y3", 60.0));

		AltInvoker alt = alt(
				"alt",
				opt("opt1", condition(pm, "{ x, y -> x > y }", "x", "y"),
						invoker("x + y + 10", pars("x", "y"))),
				opt("opt2", condition(pm, "{ x2, y2 -> x2 > y2 }", "x2", "y2"),
						invoker("x + y + 20", pars("x", "y"))),
				opt("opt3", condition(pm, "{ x3, y3 -> x3 > y3 }", "x3", "y3"),
						invoker("x + y + 30", pars("x", "y"))),
				opt("opt4", invoker("x + y + 40", pars("x", "y"))));

		add(pm, alt, get(alt, 0), get(alt, 1), get(alt, 2), get(alt, 3));

		logger.info("opt1 value : " + value(pm, "opt1"));
		assertEquals(value(pm, "opt1"), null);
		logger.info("opt2 value: " + value(pm, "opt2"));
		assertEquals(value(pm, "opt2"), 50.0);
		logger.info("opt3 value: " + value(pm, "opt3"));
		assertEquals(value(pm, "opt3"), null);
		logger.info("opt4 value: " + value(pm, "opt4"));
		assertEquals(value(pm, "opt4"), 70.0);
		logger.info("alt value: " + value(alt));
		assertEquals(value(alt), 50.0);

		put(pm, ent("x", 300.0), ent("y", 200.0));
		logger.info("alt value: " + value(alt));
		assertEquals(value(alt), 510.0);

		put(pm, ent("x", 10.0), ent("y", 20.0), ent("x2", 40.0),
				ent("y2", 50.0), ent("x3", 50.0), ent("y3", 60.0));
		logger.info("alt value: " + value(alt));
		assertEquals(value(alt), 70.0);
	}

	@Test
	public void invokerLoopTest() throws Exception {

		ParModel pm = parModel("par-model");
		add(pm, ent("x", 1));
		add(pm, par("y", invoker("x + 1", pars("x"))));
		add(pm, ent("z", inc(invoker(pm, "y"), 2)));
		Invocation z2 = invoker(pm, "z");

		ServiceInvoker iloop = loop("iloop", condition(pm, "{ z -> z < 50 }", "z"), z2);
		add(pm, iloop);
		assertEquals(value(pm, "iloop"), 48);

	}

	@Test
	public void incrementorBy1Test() throws Exception {
		ParModel pm = parModel("par-model");
		add(pm, ent("x", 1));
		add(pm, par("y", invoker("x + 1", pars("x"))));
		add(pm, ent("z", inc(invoker(pm, "y"))));

		for (int i = 0; i < 10; i++) {
			logger.info("" + value(pm, "z"));
		}
		assertTrue(value(pm, "z").equals(13));
	}

	@Test
	public void incrementorBy2Test() throws Exception {
		ParModel pm = parModel("par-model");
		add(pm, ent("x", 1));
		add(pm, par("y", invoker("x + 1", pars("x"))));
		add(pm, ent("z", inc(invoker(pm, "y"), 2)));

		for (int i = 0; i < 10; i++) {
			logger.info("" + value(pm, "z"));
		}
		assertEquals(value(pm, "z"), 24);
	}
}
