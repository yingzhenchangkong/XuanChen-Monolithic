export const getImageView = (imagePath: string) => {
  if (!imagePath) return null;
  return import.meta.env.APP_FILE_VIEW_PATH + imagePath;
}