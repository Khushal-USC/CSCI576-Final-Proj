import cv2

# Load video
video = cv2.VideoCapture('SAL.mp4')

# Get frame count
frame_count = int(video.get(cv2.CAP_PROP_FRAME_COUNT))
print(f"Frame Count: {frame_count}")

# Release the video
video.release()
