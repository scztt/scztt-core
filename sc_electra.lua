-- SC ELECTRA

print("~~~ Loading SC Electra Lua script ~~~")

inf = (1/0)

function ExponentialWarp(value, minVal, maxVal)
  return ((maxVal/minVal)^value) *  minVal
end

function LinearWarp(value, minVal, maxVal)
  return value * (maxVal - minVal) + minVal
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
    -- print(minVal)
    -- print(maxVal)
    -- print(range)
    
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

function warp(valueObject, inValue)
    -- print("Param number " .. valueObject:getMessage():getParameterNumber())

    local min = valueObject:getMin()
    local max = valueObject:getMax()
    local defaultValue = (valueObject:getDefault() - min) / (max - min)

    value = (inValue - min) / (max - min)
    -- print("Normalized values = " .. value)

    local warpIndex = valueObject:getMessage():getDeviceId()
		+ (32 * valueObject:getMessage():getParameterNumber())
    local warpInfo = warps[warpIndex]
    local warpFunc = warpInfo[1]
    local step = warpInfo[4]
    local warpedValue = warpFunc(value, warpInfo[2], warpInfo[3], warpInfo[5])
    defaultValue = warpFunc(defaultValue, warpInfo[2], warpInfo[3], warpInfo[5])

    if (step and (step > 0)) then
        warpedValue = math.floor((warpedValue / step) + 0.5) * step
        defaultValue = math.floor((defaultValue / step) + 0.5) * step

        if (step == math.floor(step)) then
            local str = (defaultValue == warpedValue) and "[%d]" or "%d"
            return string.format(str, warpedValue)
        end
    end
    
    local center = warpFunc(0.5, warpInfo[2], warpInfo[3], warpInfo[5])
    local diff = math.min(
        center - warpInfo[2],
        warpInfo[3] - center 
    )
    local isDefault = (defaultValue == warpedValue)

    if (warpFunc == DbFaderWarp) then
        warpedValue = ampdb(warpedValue)
        str = "%.1f dB"
    elseif (diff <= 0.0001) then
        str = "%.5f"
    elseif (diff <= 0.001) then
        str = "%.5f"
    elseif (diff <= 0.01) then
        str = "%.4f"
    elseif (diff <= 0.1) then
        str = "%.3f"
    elseif (diff > 10) then
        str = "%.1f"
    elseif (diff > 100) then
        str = "%.0f"
    else
        str = "%.2f"
    end

    if (isDefault) then
        str = "[" .. str .. "]"
    end

    return string.format(str, warpedValue)
end

print("~~~ End loading SC Electra Lua script ~~~")
