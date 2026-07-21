import tensorflow as tf
interpreter = tf.lite.Interpreter(model_path='d:/HocTap/Mobile/Final/Voice-assistant/app/src/main/assets/models/efficientdet_lite0.tflite')
interpreter.allocate_tensors()
for d in interpreter.get_output_details():
    print(f"index: {d['index']}, shape: {d['shape']}, name: {d['name']}")
