package luan.modules.http;

import goodjava.webserver.Request;
import goodjava.webserver.Response;
import goodjava.webserver.Handler;
import goodjava.webserver.handlers.DomainHandler;
import luan.Luan;
import luan.LuanTable;
import luan.LuanCloner;
import luan.LuanFunction;
import luan.LuanException;
import luan.modules.logging.LuanLogger;


public class LuanDomainHandler implements Handler, DomainHandler.Factory {

	private final Luan luanInit;
	private final DomainHandler domainHandler = new DomainHandler(this);

	public LuanDomainHandler(Luan luanInit) {
		LuanLogger.initThreadLogging();
		LuanCloner cloner = new LuanCloner(LuanCloner.Type.COMPLETE);
		this.luanInit = (Luan)cloner.clone(luanInit);
	}

	@Override public Handler newHandler(String domain) {
		Luan luan = newLuan(domain);
		return new LuanHandler(luan,domain);
	}

	protected Luan newLuan(final String domain) {
		LuanCloner cloner = new LuanCloner(LuanCloner.Type.COMPLETE);
		Luan luan = (Luan)cloner.clone(luanInit);
		LuanFunction reset_luan = new LuanFunction(false) {
			@Override public Object call(Object[] args) {
				domainHandler.removeHandler(domain);
				return LuanFunction.NOTHING;
			}
		};
		try {
			LuanTable Http = (LuanTable)luan.require("luan:http/Http.luan");
			Http.put( "domain", domain );
			Http.put( "reset_luan", reset_luan );
		} catch(LuanException e) {
			throw new RuntimeException(e);
		}
		return luan;
	}

	@Override public Response handle(Request request) {
		return domainHandler.handle(request);
	}
}
