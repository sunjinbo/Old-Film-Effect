attribute vec4 vPosition;
attribute vec4 inputTextureCoordinate;
varying vec2 textureCoordinate;

void main() {
    gl_Position = vPosition;
    textureCoordinate = inputTextureCoordinate.xy;
}