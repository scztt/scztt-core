-- SC ELECTRA

print("Loading SC Electra Lua script")

inf = (1/0)

function LinearWarp(value, minVal, maxVal)
  return value * (maxVal - minVal) + minVal
end

function ExponentialWarp(value, minVal, maxVal)
  return ((maxVal/minVal)^value) *  minVal
end

function CurveWarp(value, minVal, maxVal, curve)
  if (curve < 0.001 and curve > -0.001) then
     curve = 0.001
  end
  local grow = math.exp(curve)
  local a = (maxVal - minVal) / (1.0 - grow)
  local b = minVal + a

  local value = b - (a * (grow^value))
  return value
end

function FaderWarp(value, minVal, maxVal)
    if (minVal < maxVal) then
        return (value^2) * (maxVal - minVal) + minVal
    else
        return (1 - ((1 - value)^2)) * (maxVal - minVal) + minVal
    end
end

function dbamp(db)
    return 10.0^(db * 0.05);
end

function ampdb(amp)
    return (math.log(amp) / math.log(10)) * 20.0;
end

function DbFaderWarp(value, minVal, maxVal)
    minVal = dbamp(minVal)
    maxVal = dbamp(maxVal)

    local range = maxVal - minVal
    print(minVal)
    print(maxVal)
    print(range)
    
    if (range > 0) then
        return ampdb((value^2) * range + minVal)
    else
        return ampdb((1 - (1 - value)^2) * range + minVal)
    end
end

function CosineWarp(value, minVal, maxVal)
    return LinearWarp(
        0.5 - (math.cos(math.pi * value) * 0.5),
        minVal,
        maxVal
    )    
end

function SineWarp(value, minVal, maxVal)
    return LinearWarp(
        math.sin(0.5 * math.pi * value),
        minVal,
        maxVal
    )    
end

warps = {
%%%warps%%%
}

function warp(valueObject, value)
    print("Param number " .. valueObject:getMessage():getParameterNumber())

    local min = valueObject:getMin()
    local max = valueObject:getMax()

    value = (value - min) / (max - min)
    print("Normalized values = " .. value)

    local warpInfo = warps[valueObject:getMessage():getParameterNumber()]
    local warpFunc = warpInfo[1]
    local step = warpInfo[4]
    local warpedValue = warpFunc(value, warpInfo[2], warpInfo[3], warpInfo[5])

    if (step and (step > 0)) then
        warpedValue = math.floor((warpedValue / step) + 0.5) * step

        if (step == math.floor(step)) then
            return string.format("%d", warpedValue)
        end
    end
    
    return string.format("%.2f", warpedValue)
end
