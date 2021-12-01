Histogram {
	var signal, binsList, decay, scaleBySignal, <numBins;
	var normalizedIndex, histoBins, histoValues, histoValuesIntegrated, binsBuf, histoBuf, histoBufIntegrated, histoSum, normalizedIndex;

	*new {
		|signal, binsList, decay=0.9998, scaleBySignal=true|
		^super.newCopyArgs(signal, binsList, decay, scaleBySignal).init;
	}

	init {
		numBins = binsList.size - 1;
	}

	histoValues {
		^(histoValues ?? {
			histoValues = numBins.collect {
				|i|
				var value;

				value = (1/10) * signal.inRange(binsList[i], binsList[i+1]);
				if (scaleBySignal) {
					value = signal * value;
				};
				value = Integrator.kr(value, decay);
			}
		})
	}

	histoValuesIntegrated {
		^(histoValuesIntegrated ?? {
			var sum = 0;
			histoValuesIntegrated = Array.newClear(numBins);
			this.histoValues.do {
				|val, i|
				sum = sum + val;
				histoValuesIntegrated[i] = sum;
			};
			histoValuesIntegrated;
		})
	}

	binsBuf {
		^(binsBuf ?? {
			binsBuf = LocalBuf(1, binsList.size);
			SetBuf(binsBuf, binsList);
			binsBuf;
		})
	}

	histoBuf {
		var writeResult;
		^(histoBuf ?? {
			histoBuf = LocalBuf(1, numBins);
			writeResult = BufWr.kr(this.histoValues, histoBuf, 0, 0);
			histoBuf = histoBuf + (DC.kr(0) * writeResult);
		})
	}

	histoBufIntegrated {
		var writeResult, integrated;
		^(histoBufIntegrated ?? {
			histoBufIntegrated = LocalBuf(1, numBins);
			writeResult = BufWr.kr(this.histoValuesIntegrated, histoBufIntegrated, 0, 0);
			histoBufIntegrated = histoBufIntegrated + (DC.kr(0) * writeResult);
		})
	}

	histoSum {
		^(histoSum ?? {
			histoSum = this.histoValuesIntegrated.last;
		})
	}

	normalizedIndex {
		var valueIndex;
		^(normalizedIndex ?? {
			valueIndex = IndexInBetween.kr(this.binsBuf, signal);
			normalizedIndex = IndexL.kr(this.histoBufIntegrated, valueIndex) / this.histoSum;
		})
	}

	valueAtNormalizedIndex {
		|normalizedIndex|
		var valueIndex = IndexInBetween.kr(this.histoBufIntegrated, normalizedIndex * this.histoSum);
		^IndexL.kr(this.binsBuf, valueIndex);
	}
}