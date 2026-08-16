uniform float pointSize;
void main() {
    gl_Position = ftransform();
    gl_PointSize = pointSize / gl_Position.w;
}