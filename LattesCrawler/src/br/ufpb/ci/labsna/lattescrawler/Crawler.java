package br.ufpb.ci.labsna.lattescrawler;

import java.io.IOException;
import java.util.Enumeration;


/**
 * @author Alexandre N�brega Duarte - alexandre@ci.ufpb.br - http://alexandrend.com
 * 		   Marcilio Olinto de Oliveira Lemos - marcilio.cc.lemos@gmail.com - http://github.com/marcilioLemos 
 * 
 * Sintaxe para uso:  java LattesCrawler arquivo_de_entrada profundidade anoMaximo 
 * 
 * Onde arquivo_de_entrada contem um ou mais identificadores n�meros de curr�culos lattes,
 * profundidade � um n�mero inteiro indicando a dist�ncia a partir do curr�culo inicial que deve ser percorrida
 * e anoMaximo é um  inteiro representando um ano e que o sistema só considere as produções e conexões estabelecidas até aquele ano.
 * 
 * A sa�da ser� impressa na sa�da padr�o e representa uma descri��o em formato GML de um grafo.
 * 
 */

class Crawler implements Runnable {

	private LattesCrawler lc;
	
	private String lattesID;
	
	private int maxYear;

	
	public Crawler(LattesCrawler lc, String lattesID, int year) {
		this.lc = lc;
		this.lattesID = lattesID;
		this.maxYear=year;
	}
	
	public void run(){
		
			//System.err.println( "START Thread" + this);
		
			
			//N�o queremos sobrecarregar o site do CNPQ. :-)
			try {
				Thread.sleep( (long) (Math.random() * 2000) );
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			        
			Lattes l = new Lattes(lattesID);
			try {
				l.extractData(lc,maxYear); //enxtrai lates do crwaler
				lc.addLattes(l);
				
				Enumeration <String> con = l.getConnections().keys();
				while( con.hasMoreElements()) {
					String ol = con.nextElement();
					if( !lc.visited(ol)) {
						lc.addSeed(ol);				
					}
				}
			} catch (IOException e) {
				System.err.println( e + " http://lattes.cnpq.br/" + lattesID);
	
			} catch (LattesNotFoundException e) {
				// TODO Auto-generated catch block
				System.err.println(e);
			}
			
			//System.err.println( "END Thread" + this);
			

		
	}

}