export const lonToTileX = (lon, z) => {
  const n = Math.pow(2, z);
  return Math.floor(((lon + 180) / 360) * n);
};

export const latToTileY = (lat, z) => {
  const n = Math.pow(2, z);
  const latRad = (lat * Math.PI) / 180;
  return Math.floor(
    ((1 - Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI) / 2) * n
  );
};

export const calculateVisibleTiles = (minLat, maxLat, minLng, maxLng, zoom) => {
  const xMin = lonToTileX(minLng, zoom);
  const xMax = lonToTileX(maxLng, zoom);
  const yMin = latToTileY(maxLat, zoom); 
  const yMax = latToTileY(minLat, zoom);

  const tiles = [];
  for (let x = xMin; x <= xMax; x++) {
    for (let y = yMin; y <= yMax; y++) {
      tiles.push({ z: zoom, x, y });
    }
  }

  return tiles;
};