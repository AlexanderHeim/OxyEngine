package OxyEngine.Core.Renderer.Buffer;

public abstract class TangentBuffer extends Buffer {

    protected float[] biAndTangent = new float[0];

    protected final BufferLayoutProducer.BufferLayoutImpl implementation;

    public TangentBuffer(BufferLayoutProducer.BufferLayoutImpl template) {
        this.implementation = template;
    }

    public void setBiAndTangent(float[] biAndTangent) {
        this.biAndTangent = biAndTangent;
    }
}
