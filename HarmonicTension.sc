/* Classe Supercollider para Cálculo de Tensão Harmônica usando algorítimo de Rugosidade de Plomp e Levelt.
**
** and some calculate methodes based in
**
**  FERGUSON, S. Concerto for piano and orhestra. Phd Thesis — Faculty of Music,
**  McGuill University, Montreal, December 2000.
**
** HarmTension.new([freq1,freq2,freq...]); versão com Harmônicos em Temperamento Igual
**
** HarmTensionNT.new([freq1,freq2,freq...]); versão com Harmônicos escala Física
**
** a classe gera automaticamente a série harmônica com n parciais e array de amplitudes h = 1/n (amplitudes decrescentes)
**
**
** Designed by Rael B. Gimenes <rael dot gimenes at gmail dot com>
**
**
*/

HarmonicTension {
	
	var <>nHarmonics = 10, <>notes;
	var <>arrayNotes, <>arrayAmps, <>arrayRoughness; 
	var arrayNotesTemp, arrayAmpsTemp, posForClean, sizeForClean;
	var <>multiply = false;

	*new { arg args;
	^super.new.init(args);
	}
	
	init { | newNotes |
	 
	this.notes_(newNotes);
	}

	serieHarmonica { arg myNotes;
	arrayNotes = Array.fill(250,0);
	arrayAmps = Array.fill(250,0);
	arrayNotesTemp = Array.fill(250,0); // cria big array para mesclar todas as notas
	arrayAmpsTemp = Array.fill(250,0); // cria big array para mesclar todas as intensidades
	myNotes.do({ arg itemColecao;
				nHarmonics.do({arg countHarm;							// monta serie harmonica de cada item da colecao e coloca na posicao do big array
					var freqHz, freqHzFinal, posicao;
					freqHz = itemColecao.midicps;					// transforma o item da coleção em Hz.
					freqHzFinal = (freqHz*(countHarm+1)).cpsmidi.round;		// calcula cada um dos harmônicos
					posicao = freqHzFinal;						// posição do array para colocar a nota
					arrayNotesTemp.put(posicao,freqHzFinal);			// coloca o harmônico no big array				
					if ((arrayAmpsTemp[posicao] != 0),					// verifica se tem alguma harmonica naquela posicao do big array de amplitudes
							{ var amp1, amp2, ampFinal;				// se sim colocar raiz quadrada da soma dos quadrados de amp1 e amp2
								amp1 = arrayAmpsTemp[posicao];
								amp2 = 1/(countHarm+1);
								ampFinal = sqrt((amp1*amp1)+(amp2*amp2));
								if (ampFinal > 1, {ampFinal = 1},{});  // se amplitude final maior que 1 então ampFinal = 1;
								arrayAmpsTemp.put(posicao,ampFinal)},
							{arrayAmpsTemp.put(posicao,1/(countHarm+1))});	// se posição não tem harmonico coloca amplitude calculada do harmonico.
					});

		});
		posForClean = 0;
		sizeForClean = 0;
		arrayNotesTemp.do({ arg item, i; 
 				if ((item != 0), { arrayNotes.put(posForClean,item); posForClean = posForClean+1; sizeForClean=sizeForClean+1;},{});
				});
				arrayNotes = arrayNotes.copyRange(0,(sizeForClean-1));
		
		posForClean = 0;
		sizeForClean = 0;
		arrayAmpsTemp.do({ arg item, i; 
 				if ((item != 0), { arrayAmps.put(posForClean,item); posForClean = posForClean+1; sizeForClean=sizeForClean+1;},{});
				});
				arrayAmps = arrayAmps.copyRange(0,(sizeForClean-1));
		
		}
		

	
/*	view { 
	var graphic,arrayPlot;
	graphic = GNUPlot.new;
	arrayPlot = Array.fill(10,0);
	10.do({arg i ; arrayPlot.put(i,[overtones[i],amplitudes[i]])});
	graphic.scatter(arrayPlot);
	}
*/
	
	help { 		"Uso da Classe:
			
			var = HarmonicTension([note, note, note, note,..])  De uma a quantas notas quiser
			
			Para verificar valores:
			
			instance.arrayNotes (mostra o Array com o espectro de notas e series harmonicas)
			instance.arrayAmps (mostra amplitudes de cada nota)
			instance.arrayRoughness (mostra o array depois de calcular Roughness)
			instance.roughness (calcula a rugosidade da colecao)
			instance.nHarmonics (quantidade de harmonicos utilizados no calculo) (default: 10)
			instance.nHarmonics = (x)   modifica a quantidade de harmonicos usados para calculo
			instance.help (mostra essa mensagem)
			instance.multiply = true;   Multiplica valor da rugosidade pelo produto das amplitudes (algoritmo de Ferguson)
			instance.multiply = false; Não multiplica.
			
			".postln;
	}
	
	centralFreq { arg freq1,freq2;
			var cf;
			// freq1=freq1-12;freq2=freq2-12; // Parncutt Midinote
			cf = (freq1.midicps+freq2.midicps)/2;
			//"  central freq = ".post; cf.postln;
			^cf;
	}

	cbw { arg cf, freq1,freq2, amp1, amp2;
		var tempBc, bandaCritica, difFreq;
		// freq1=freq1-12;freq2=freq2-12; // ParncuttMidinote
		tempBc = 1.72*(cf**0.65);
		//"tempBC = ".post; tempBc.postln;
		difFreq = (freq2.midicps-freq1.midicps);
		//"freq 1 = ".post; freq1.midicps.post; "... freq 2 = ".post;freq2.midicps.post; " ... Dif freq = ".post; difFreq.postln;
		bandaCritica = (difFreq/tempBc);
		if (bandaCritica > 1.2,{bandaCritica=0},{});
		//"banda critica = ".post; bandaCritica.postln;		
		^bandaCritica;
		
	}

	roughTable { arg cbw, amp1, amp2;
	var tempCbw;
	tempCbw = 	(((2.7182818*cbw)/0.25)*exp((cbw*(-1))/0.25))**2;
	//"valor de rugosidade = ".post; tempCbw.postln;	
	//"table = ".post; (tempCbw*(amp1*amp2)).postln;	
	if (multiply, {^tempCbw*(amp1*amp2);}, {^tempCbw}); // se multiply = true: multiplica valor da tabela pelas amplitudes (igual algoritmo Ferguson)
								// caso contrário deixa como está.
	}

	roughness { var arraySize, countInt, freq1, freq2, amp1, amp2, cfreq, cb, rough, high, calcRough, calcAmp, somaAmps, finalRough;
			arrayRoughness = Array.new;
			this.serieHarmonica(this.notes);
			arraySize = arrayNotes.size;
			//"Array Size = ".post; arraySize.postln; 			
			(arraySize-1).do({arg low;
					//"Low = ".post; low.postln;
					for ((low+1), (arraySize-1), {arg high; //"high = ".post; high.postln;
						freq1 = arrayNotes[low]; //"freq 1 = ".post; freq1.post; " | ".post;
						freq2 = arrayNotes[high];// "freq 2 = ".post; freq2.post; " | ".post;
						amp1 = arrayAmps[low]; //"amp 1 = ".post; amp1.post; " | ".post;
						amp2 = arrayAmps[high]; //" amp2 = ".post; amp2.post; " | ".postln;
						cfreq = this.centralFreq(freq1,freq2);
						cb = this.cbw(cfreq,freq1,freq2, amp1, amp2);
						rough = this.roughTable(cb, amp1,amp2);
						this.arrayRoughness = this.arrayRoughness ++ rough;
					});
			
			});
	somaAmps = (this.arrayAmps*this.arrayAmps);
	calcRough = this.arrayRoughness.sum;
	calcAmp = somaAmps.sum;
	finalRough = (calcRough/calcAmp);
	//"Rugosidade final = ".post; finalRough.postln;	
	^finalRough;	
	}

	


}
                                
HarmonicTensionNT {
	
	var <>nHarmonics = 10, <>notes;
	var <>arrayNotes, <>arrayAmps, <>arrayRoughness; 
	var arrayNotesTemp, arrayAmpsTemp, posForClean, sizeForClean;
	var <>multiply = false;

	*new { arg args;
	^super.new.init(args);
	}
	
	init { | newNotes |
	this.notes_(newNotes);
	this.serieHarmonica(notes);
	}

	serieHarmonica { arg myNotes;
	arrayNotes = Array.fill(250,0);
	arrayAmps = Array.fill(250,0);
	arrayNotesTemp = Array.fill(125500,0); // cria big array para mesclar todas as notas
	arrayAmpsTemp = Array.fill(1255500,0); // cria big array para mesclar todas as intensidades
	myNotes.do({ arg itemColecao;
				nHarmonics.do({arg countHarm;			// monta serie harmonica de cada item da colecao e coloca na posicao do big array
					var freqHz, freqHzFinal, posicao;
					freqHz = itemColecao.midicps;				// transforma o item da coleção em Hz.
					freqHzFinal = (freqHz*(countHarm+1));		// calcula cada um dos harmônicos
					posicao = freqHzFinal.asInteger;						// posição do array para colocar a nota
					arrayNotesTemp.put(posicao,freqHzFinal);			// coloca o harmônico no big array				
					if ((arrayAmpsTemp[posicao] != 0),					// verifica se tem alguma harmonica naquela posicao do big array de amplitudes
							{ var amp1, amp2, ampFinal;				// se sim colocar raiz quadrada da soma dos quadrados de amp1 e amp2
								amp1 = arrayAmpsTemp[posicao];
								amp2 = 1/(countHarm+1);
								ampFinal = sqrt((amp1*amp1)+(amp2*amp2));
								if (ampFinal > 1, {ampFinal = 1},{});  // se amplitude final maior que 1 então ampFinal = 1;
								arrayAmpsTemp.put(posicao,ampFinal)},
							{arrayAmpsTemp.put(posicao,1/(countHarm+1))});	// se posição não tem harmonico coloca amplitude calculada do harmonico.
					});

		});
		posForClean = 0;
		sizeForClean = 0;
		arrayNotesTemp.do({ arg item, i; 
 				if ((item != 0), { arrayNotes.put(posForClean,item); posForClean = posForClean+1; sizeForClean=sizeForClean+1;},{});
				});
				arrayNotes = arrayNotes.copyRange(0,(sizeForClean-1));
		
		posForClean = 0;
		sizeForClean = 0;
		arrayAmpsTemp.do({ arg item, i; 
 				if ((item != 0), { arrayAmps.put(posForClean,item); posForClean = posForClean+1; sizeForClean=sizeForClean+1;},{});
				});
				arrayAmps = arrayAmps.copyRange(0,(sizeForClean-1));
		
		}
		

	
/*	view { 
	var graphic,arrayPlot;
	graphic = GNUPlot.new;
	arrayPlot = Array.fill(10,0);
	10.do({arg i ; arrayPlot.put(i,[overtones[i],amplitudes[i]])});
	graphic.scatter(arrayPlot);
	}
*/
	
	help { 		"Uso da Classe:
			
			var = HarmonicTension([note, note, note, note,..])  De uma a quantas notas quiser
			
			Para verificar valores:
			
			instance.arrayNotes (mostra o Array com o espectro de notas e series harmonicas)
			instance.arrayAmps (mostra amplitudes de cada nota)
			instance.arrayRoughness (mostra o array depois de calcular Roughness)
			instance.roughness (calcula a rugosidade da colecao)
			instance.nHarmonics (quantidade de harmonicos utilizados no calculo) (default: 10)
			instance.nHarmonics = (x)   modifica a quantidade de harmonicos usados para calculo
			instance.help (mostra essa mensagem)
			instance.multiply = true;   Multiplica valor da rugosidade pelo produto das amplitudes (algoritmo de Ferguson)
			instance.multiply = false; Não multiplica.
			
			".postln;
	}
	
	centralFreq { arg freq1,freq2;
			var cf;
			// freq1=freq1-12;freq2=freq2-12; // Parncutt Midinote
			cf = (freq1+freq2)/2;
			//"  central freq = ".post; cf.postln;
			^cf;
	}

	cbw { arg cf, freq1,freq2, amp1, amp2;
		var tempBc, bandaCritica, difFreq;
		// freq1=freq1-12;freq2=freq2-12; // ParncuttMidinote
		tempBc = 1.72*(cf**0.65);
		//"tempBC = ".post; tempBc.postln;
		difFreq = (freq2-freq1);
		//"freq 1 = ".post; freq1.midicps.post; "... freq 2 = ".post;freq2.midicps.post; " ... Dif freq = ".post; difFreq.postln;
		bandaCritica = (difFreq/tempBc);
		if (bandaCritica > 1.2,{bandaCritica=0},{});
		//"banda critica = ".post; bandaCritica.postln;		
		^bandaCritica;
		
	}

	roughTable { arg cbw, amp1, amp2;
	var tempCbw;
	tempCbw = 	(((2.7182818*cbw)/0.25)*exp((cbw*(-1))/0.25))**2;
	//"valor de rugosidade = ".post; tempCbw.postln;	
	//"table = ".post; (tempCbw*(amp1*amp2)).postln;	
	if (multiply, {^tempCbw*(amp1*amp2);}, {^tempCbw}); // se multiply = true: multiplica valor da tabela pelas amplitudes (igual algoritmo Ferguson)
								// caso contrário deixa como está.
	}

	roughness { var arraySize, countInt, freq1, freq2, amp1, amp2, cfreq, cb, rough, high, calcRough, calcAmp, somaAmps, finalRough;
			arrayRoughness = Array.new;
			//this.serieHarmonica(this.notes);
			arraySize = arrayNotes.size;
			//"Array Size = ".post; arraySize.postln; 			
			(arraySize-1).do({arg low;
					//"Low = ".post; low.postln;
					for ((low+1), (arraySize-1), {arg high; //"high = ".post; high.postln;
						freq1 = arrayNotes[low]; //"freq 1 = ".post; freq1.post; " | ".post;
						freq2 = arrayNotes[high];// "freq 2 = ".post; freq2.post; " | ".post;
						amp1 = arrayAmps[low]; //"amp 1 = ".post; amp1.post; " | ".post;
						amp2 = arrayAmps[high]; //" amp2 = ".post; amp2.post; " | ".postln;
						cfreq = this.centralFreq(freq1,freq2);
						cb = this.cbw(cfreq,freq1,freq2, amp1, amp2);
						rough = this.roughTable(cb, amp1,amp2);
						this.arrayRoughness = this.arrayRoughness ++ rough;
					});
			
			});
	somaAmps = (this.arrayAmps*this.arrayAmps);
	calcRough = this.arrayRoughness.sum;
	calcAmp = somaAmps.sum;
	finalRough = (calcRough/calcAmp);
	//"Rugosidade final = ".post; finalRough.postln;	
	^finalRough;	
	}

	


}



