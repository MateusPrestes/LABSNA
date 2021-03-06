package br.ufpb.ci.labsna.lattescrawler;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.omg.CORBA.portable.InputStream;

/**
 * @author Alexandre N�brega Duarte - alexandre@ci.ufpb.br - http://alexandrend.com
 */
public class Lattes {

	//Utilizado para tratar lattes de h�monimos, o que pode gerar problemas com o processamento do grafo.
	private static Map<String,Integer>cvNames = Collections.synchronizedMap(new HashMap<String,Integer> ()); 
	
	
	private String lattesID;
	private Dictionary <String,Integer>connections; //conexões de um lattes para outro
	private String name; //nome
	private String nivel; //nivel pesquisador cnpq
	private int artigosPeriod; //artigos
	private int artigosConfer;
	private int orientaMestrado;
	private int orientaDoutorado;
	private int bancasMestrado;
	private int bancasDoutorado;
	
	public Lattes(String lattesID) {
		this.lattesID = lattesID;
		connections = new Hashtable<String,Integer>();
		artigosPeriod=0;
		artigosConfer=0;
		orientaMestrado=0;
		orientaDoutorado=0;
		bancasDoutorado=0;
		bancasMestrado=0;
		
	}

	public void addConnection(String otherLattesID) {

		Integer i = (Integer)connections.get(otherLattesID);
		if( i == null ) i = new Integer(0);
		
		i = i + 1;		
		connections.put(otherLattesID,i);
	}

	public int getBancasMestrado() {
		return bancasMestrado;
	}

	public void setBancasMestrado(int bancasMestrado) {
		this.bancasMestrado = bancasMestrado;
	}

	public int getBancasDoutorado() {
		return bancasDoutorado;
	}

	public void setBancasDoutorado(int bancasDoutorado) {
		this.bancasDoutorado = bancasDoutorado;
	}

	public void setName(String name) {
			
		Integer i = Lattes.cvNames.get(name);
		
		if( i == null ) {
			this.name = name;
			Lattes.cvNames.put(name, new Integer(1));
		} else {
			this.name = name + " (" + i + ")";
			i = i + 1;
			Lattes.cvNames.put(name, i);
		}
			
	}
	
	public String getName(){
		return name;
	}
	
	public String getLattesID() {
		return lattesID;
	}
	
	public Dictionary <String,Integer> getConnections() {
		return connections;
	}

	public void setPQ(String nivelPQ) {
		setNivel("PQ-"+nivelPQ);
	}

	public void setDT(String nivelDT) {
		setNivel( "DT-"+nivelDT);
		
	}
	
	public void setNivel(String nivel) {
		this.nivel = nivel;
	}
	
	public String getNivel(){
		return this.nivel;
	}

	public void extractData(LattesCrawler lc, int maxYear) throws IOException, LattesNotFoundException  {
			
			Document doc = Jsoup.connect("http://lattes.cnpq.br/" + lattesID).timeout(0).get();
			
			String title[] = doc.select(".nome").text().split("Bolsista"); 
			
			if( title[0].length() < 2) {
					//System.err.println( doc.text());
					throw new LattesNotFoundException( "http://lattes.cnpq.br/" + lattesID);
			}
			
			setName(title[0]);
			this.nivel = null;

	
			if( title.length > 1 ) //é bolsista
				this.nivel =  "Bolsista " + title[1];
			
			
			Elements divs; 
			
			/* produção em periodicos */
			
			if(maxYear==0){
				setArtigosPeriod(doc.select("div#artigos-completos div.artigo-completo").size());
			}else{
				divs = doc.select("div#artigos-completos  span[data-tipo-ordenacao=ano]");
				int numArtigosP=0;
				for(int i = 0;i<divs.size();i++){
					int year=Integer.parseInt(divs.get(i).text());
					if(year<=maxYear)
						numArtigosP++;
				}				
				setArtigosPeriod(numArtigosP);
			}
			
			
			/* produção em anais de congresso */
			
			divs =	doc.select("div.cita-artigos:has(a[name=TrabalhosPublicadosAnaisCongresso])");
			int numeroArtigos = 0;
			for(int i = 0;i<divs.size();i++){
				
				Element divPublicacao = divs.get(i).nextElementSibling();
				while(true){
					
					if(divPublicacao!=null){
					
						String div_class = divPublicacao.hasAttr("class")? divPublicacao.attr("class"):""; 
						
						if(div_class.equals("cita-artigos")){
							break; //acabou uma categoria de artigos (completos, resumos, etc) 
						}else
							if(div_class.equals("layout-cell layout-cell-11")){
								if(maxYear==0)
									numeroArtigos++;
								else{
									String text =divPublicacao.select("div.layout-cell-pad-5").get(0).text();
									Pattern ORIENT_REGEX = Pattern.compile("In:[\\s\\S]*?(\\d{4})");
									Matcher matcher =  ORIENT_REGEX.matcher(text);
									
									while(matcher.find()){ //verifica se os anos são iguais
										
										ORIENT_REGEX= Pattern.compile("(\\d{4})");
										Matcher matcherYear=ORIENT_REGEX.matcher(matcher.group());
										matcherYear.find();
										int year=Integer.parseInt(matcherYear.group());;
										if(year<=maxYear){
										  numeroArtigos++;		
										}		
										break;	
										
									
									}//end match
									
									
								}//end else
							}
						divPublicacao = divPublicacao.nextElementSibling();
					  }else
						  break;
				
				  }//end while
				
			}//end for
			
			setArtigosConfer(numeroArtigos);
			
			
			/* orientações de mestrado concluidas */
			
			
			divs=doc.select("a[name=Orientacoesconcluidas]~div.cita-artigos:has(b:containsOwn(mestrado))");
			int numOrientM=0;
			if(!divs.isEmpty()){
				Element divOrientM= divs.get(0);
				do{
					divOrientM=divOrientM.nextElementSibling();
					if(divOrientM!=null){
						String div_class = divOrientM.hasAttr("class")? divOrientM.attr("class"):""; 
						
						if(div_class.equals("cita-artigos")){
							break; //acabou as orientacoes 
						}else
							if(div_class.equals("layout-cell layout-cell-11")){
								if(maxYear==0)
									numOrientM++;
								else{
									String text =divOrientM.select("div.layout-cell-pad-5").get(0).text();
									Pattern ORIENT_REGEX = Pattern.compile("(\\d{4})[\\s\\S]*Disserta");
									Matcher matcher =  ORIENT_REGEX.matcher(text);
									while(matcher.find()){
										ORIENT_REGEX= Pattern.compile("(\\d{4})");
										Matcher matcherYear=ORIENT_REGEX.matcher(matcher.group());
										while(matcherYear.find()){
											int year=Integer.parseInt(matcherYear.group());;
											if(year<=maxYear)
												numOrientM++;
											}//end year
									}//end match
									
								}//end else
							}
					}else
						break;
				}while(true);
			}
			setOrientaMestrado(numOrientM);
			
			/* orientações de doutorado concluidas */
			
			divs=doc.select("a[name=Orientacoesconcluidas]~div.cita-artigos:has(b:containsOwn(doutorado))");
			int numOrientD=0;
			if(!divs.isEmpty()){
				Element divOrientD= divs.get(0);
				do{
					divOrientD=divOrientD.nextElementSibling();
					if(divOrientD!=null){
						String div_class = divOrientD.hasAttr("class")? divOrientD.attr("class"):""; 
						
						if(div_class.equals("cita-artigos")){
							break; //acabou as orientacoes 
						}else
							if(div_class.equals("layout-cell layout-cell-11")){
								if(maxYear==0)
									numOrientD++;
								else{
									String text =divOrientD.select("div.layout-cell-pad-5").get(0).text();
									Pattern ORIENT_REGEX = Pattern.compile("(\\d{4})[\\s\\S]*Tese");
									Matcher matcher =  ORIENT_REGEX.matcher(text);
									while(matcher.find()){
										ORIENT_REGEX= Pattern.compile("(\\d{4})");
										Matcher matcherYear=ORIENT_REGEX.matcher(matcher.group());
										while(matcherYear.find()){
											int year=Integer.parseInt(matcherYear.group());;
											if(year<=maxYear)
												numOrientD++;
											}//end year
									}//end match
									
								}//end else
							}
					}else
						break;
				}while(true);
			}
			setOrientaDoutorado(numOrientD);
			
			/* participações em bancas de mestrado */
			
			divs=doc.select("a[name=ParticipacaoBancasTrabalho]~div.cita-artigos:has(b:containsOwn(Mestrado))");
			int numBancasM=0;//
			if(!divs.isEmpty()){
				Element divBancaM= divs.get(0);
				do{
					divBancaM=divBancaM.nextElementSibling();
					if(divBancaM!=null){
						String div_class = divBancaM.hasAttr("class")? divBancaM.attr("class"):""; 
						
						if(div_class.equals("cita-artigos")){
							break; //acabou as orientacoes 
						}else
							if(div_class.equals("layout-cell layout-cell-11")){
								if(maxYear==0)
									numBancasM++;
								else{
									String text =divBancaM.select("div.layout-cell-pad-5").get(0).text();
									Pattern ORIENT_REGEX = Pattern.compile("(\\d{4})[\\s\\S]*Disserta");
									Matcher matcher =  ORIENT_REGEX.matcher(text);
									while(matcher.find()){
										ORIENT_REGEX= Pattern.compile("(\\d{4})");
										Matcher matcherYear=ORIENT_REGEX.matcher(matcher.group());
										while(matcherYear.find()){
											int year=Integer.parseInt(matcherYear.group());;
											if(year<=maxYear)
												numBancasM++;
											}//end year
									}//end match
									
								}
							}
					}else
						break;
				}while(true);
			}
			setBancasMestrado(numBancasM);
			
			/* participações em bancas de doutorado */
			
			divs=doc.select("a[name=ParticipacaoBancasTrabalho]~div.cita-artigos:has(b:containsOwn(Teses de doutorado))");
			int numBancasD=0;//
			if(!divs.isEmpty()){
				Element divBancaD= divs.get(0);
				do{
					divBancaD=divBancaD.nextElementSibling();
					if(divBancaD!=null){
						String div_class = divBancaD.hasAttr("class")? divBancaD.attr("class"):""; 
						
						if(div_class.equals("cita-artigos")){
							break; //acabou as orientacoes 
						}else
							if(div_class.equals("layout-cell layout-cell-11")){
								if(maxYear==0)
									numBancasD++;
								else{
									String text =divBancaD.select("div.layout-cell-pad-5").get(0).text();
									Pattern ORIENT_REGEX = Pattern.compile("(\\d{4})[\\s\\S]*Tese");
									Matcher matcher =  ORIENT_REGEX.matcher(text);
									while(matcher.find()){
										ORIENT_REGEX= Pattern.compile("(\\d{4})");
										Matcher matcherYear=ORIENT_REGEX.matcher(matcher.group());
										while(matcherYear.find()){
											int year=Integer.parseInt(matcherYear.group());;
											if(year<=maxYear)
												numBancasD++;
											}//end year
									}//end match
									
								}//end else
							}
					}else
						break;
				}while(true);
			}
			setBancasDoutorado(numBancasD);
			
			
			
			/*  adiciona conexoes */
			
			if(maxYear==0){
				
				Elements links = doc.select("a[href]");	
				for (Element link : links) {
				    String l = link.attr("abs:href"); 
				    
				    if( l.startsWith("http://lattes.cnpq.br") && !l.endsWith(lattesID) && l.substring(22).length() == 16) {
				    	addConnection( l.substring(22)); 
				    }
				}
			}else{
				
				divs = doc.select("div.layout-cell-pad-5:has(a[href])");
				for(Element div: divs){ //não pega conexões sem datas ou que possuam mais de um ano no texto =(
					
					Elements links = div.select("a[href]");
					for(Element link : links){
						
						String l = link.attr("abs:href"); 
					    
					    if( l.startsWith("http://lattes.cnpq.br") && !l.endsWith(lattesID) && l.substring(22).length() == 16) {
					    	
					    	String text = div.text();
							Pattern ORIENT_REGEX = Pattern.compile("(\\d{4})");
							Matcher matcher =  ORIENT_REGEX.matcher(text);
							boolean isFirst=true; //flag apenas para a primeira inicializacao
							int year=-1,lastYear=-1;
							
							while(matcher.find()){
								
								year = Integer.parseInt(matcher.group());
								
								if(isFirst){
									lastYear=year;
									isFirst=false;
								}
								
								if(year!=lastYear){ //nao tem como saber qual o ano
									year=-1;
									break;
								}
								
								lastYear=year;
								
							}//end while			
							
							if(year>0&&year<=maxYear){
								addConnection( l.substring(22));
							}
					    	 
					    }//end if links
					
					}// end for links
						
				}//end for divs
				
			}//end else year
			
			
				
				
						
		    
	}
	
	
	public boolean equals (Lattes o) {
		return o.getLattesID().equals(getLattesID());
	}

	public int getArtigosPeriod() {
		return artigosPeriod;
	}

	public void setArtigosPeriod(int artigos_periodico) {
		this.artigosPeriod = artigos_periodico;
	}

	public int getArtigosConfer() {
		return artigosConfer;
	}

	public void setArtigosConfer(int artigosConfer) {
		this.artigosConfer = artigosConfer;
	}

	public int getOrientaMestrado() {
		return orientaMestrado;
	}

	public void setOrientaMestrado(int orientaMestrado) {
		this.orientaMestrado = orientaMestrado;
	}

	public int getOrientaDoutorado() {
		return orientaDoutorado;
	}

	public void setOrientaDoutorado(int orientaDoutorado) {
		this.orientaDoutorado = orientaDoutorado;
	}
	
}
