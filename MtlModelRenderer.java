package renderEngine;

import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Matrix4f;

import entities.Camera;
import entities.Entity;
import entities.Light;
import models.RawModel;
import models.TexturedModel;
import shaders.MtlShader;
import textures.Material;
import toolBox.Maths;

public class MtlModelRenderer {

	private MtlShader shader;

	public MtlModelRenderer(Matrix4f projectionMatrix) {
		this.shader = new MtlShader();
		shader.start();
		shader.loadProjectionMatrix(projectionMatrix);
		
		//shader.connectTextureUnits();
		
		shader.stop();
	}

/*	public void render(Map<TexturedModel, List<Entity>> entities, Vector4f clipPlane, List<Light> lights, Camera camera) {
		shader.start();
		prepare(clipPlane, lights, camera);
		for (TexturedModel model : entities.keySet()) {
			prepareTexturedModel(model);
			List<Entity> batch = entities.get(model);
			for (Entity entity : batch) {
				prepareInstance(entity);
				GL11.glDrawElements(GL11.GL_TRIANGLES, model.getRawModel().getVertexCount(), GL11.GL_UNSIGNED_INT, 0);
			}
			unbindTexturedModel();
		}
		shader.stop();
	}*/
	
	public void render(Map<TexturedModel, List<Entity>> entities, List<Light> lights, Camera camera) {
		shader.start();
		
		//prepare(clipPlane, lights, camera);
		
		prepare(lights, camera);

		
		for(TexturedModel model:entities.keySet()){
			prepareOBJMTLModel(model);

			List<Entity> batch = entities.get(model);

			for(Entity entity:batch){
				prepareInstance(entity);

				//texture set kat sini
				//int matCount = model.getMaterialCount();

				//ni percubaan nak lukis sume ikut faces punya susunan 
				String[] matName = model.getMaterialsOrderArray().get(model.getFileName());

				//ni klu nak lukis berkumpulan ikut bape bnyk material dia ada
				//String[] matName = model.getMaterialsNameArray();

				//Material mtl = new Material();

				//arahan untuk lukis face = 3 indices satu masa 


				for(int i = 0; i<model.getRawModel().getVertexCount(); i += 3){

					//kalau tak buat static dia tak dapat hantar ke sini 
					Material mtl = TexturedModel.materialsHM.get(matName[i/3]);

					if( mtl != null){
						shader.loadMtlShininess(mtl.shininess);
						shader.loadMtlAmbientColour(mtl.ambientColour);
						shader.loadMtlDiffuseColour(mtl.diffuseColour);
						shader.loadMtlSpecularColour(mtl.specularColour);
						shader.loadMtlIndexOfRefraction(mtl.indexOfRefraction);
						shader.loadMtlTransparency(mtl.transparency);
						shader.loadMtlIllumination(mtl.illumination);

						GL13.glActiveTexture(GL13.GL_TEXTURE0);
						GL11.glBindTexture(GL11.GL_TEXTURE_2D, mtl.textureID);

					}
					/*else{
							Material defaultMtl = TexturedModel.materialsHM.get("FrontColor");

							shader.loadMtlShininess(defaultMtl.shininess);
							shader.loadMtlAmbientColour(defaultMtl.ambientColour);
							shader.loadMtlDiffuseColour(defaultMtl.diffuseColour);
							shader.loadMtlSpecularColour(defaultMtl.specularColour);
							shader.loadMtlIndexOfRefraction(defaultMtl.indexOfRefraction);
							shader.loadMtlTransparency(defaultMtl.transparency);
							shader.loadMtlIllumination(defaultMtl.illumination);

							GL13.glActiveTexture(GL13.GL_TEXTURE0);
							GL11.glBindTexture(GL11.GL_TEXTURE_2D, defaultMtl.textureID);
						}		*/		

					//lukis satu face sekali - 3 indices 
					//the last parameter is offset in bytes --> offset*4 (int -> 32 bits dakara 4 bytes)

					GL11.glDrawElements(GL11.GL_TRIANGLES, 3, GL11.GL_UNSIGNED_INT, i * 4);	

				}

			}
			unbindTexturedModel();
		}
		shader.stop();
	}
	
	public void cleanUp(){
		shader.cleanUp();
	}
	
	private void prepareOBJMTLModel(TexturedModel model){

		//
		RawModel rawModel = model.getRawModel();

		GL30.glBindVertexArray(rawModel.getVaoID());

		GL20.glEnableVertexAttribArray(0); //position
		GL20.glEnableVertexAttribArray(1); //texcoords
		GL20.glEnableVertexAttribArray(2); //normals

		//shader.loadUseMtl(model.getUseMtl());
		//}
	}

	/*private void prepareTexturedModel(TexturedModel model) {
		RawModel rawModel = model.getRawModel();
		GL30.glBindVertexArray(rawModel.getVaoID());
		GL20.glEnableVertexAttribArray(0);
		GL20.glEnableVertexAttribArray(1);
		GL20.glEnableVertexAttribArray(2);
		GL20.glEnableVertexAttribArray(3);
		ModelTexture texture = model.getTexture();
		shader.loadNumberOfRows(texture.getNumberOfRows());
		if (texture.isHasTransparency()) {
			MasterRenderer.disableCulling();
		}
		shader.loadShineVariables(texture.getShineDamper(), texture.getReflectivity());
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, model.getTexture().getID());
		GL13.glActiveTexture(GL13.GL_TEXTURE1);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, model.getTexture().getNormalMap());
	}*/

	private void unbindTexturedModel() {
		MasterRenderer.enableCulling();
		GL20.glDisableVertexAttribArray(0);
		GL20.glDisableVertexAttribArray(1);
		GL20.glDisableVertexAttribArray(2);
		
		//GL20.glDisableVertexAttribArray(3);
		
		GL30.glBindVertexArray(0);
	}

	private void prepareInstance(Entity entity) {
		Matrix4f transformationMatrix = Maths.createTransformationMatrix(entity.getPosition(), entity.getRotX(), entity.getRotY(), entity.getRotZ(), entity.getScale());

		shader.loadTransformationMatrix(transformationMatrix);
		
		//shader.loadOffset(entity.getTextureXOffset(), entity.getTextureYOffset());
	}

/*	private void prepare(Vector4f clipPlane, List<Light> lights, Camera camera) {
		shader.loadClipPlane(clipPlane);
		//need to be public variables in MasterRenderer
		shader.loadSkyColour(MasterRenderer.RED, MasterRenderer.GREEN, MasterRenderer.BLUE);
		Matrix4f viewMatrix = Maths.createViewMatrix(camera);
		
		shader.loadLights(lights, viewMatrix);
		shader.loadViewMatrix(viewMatrix);
	}*/
	private void prepare(List<Light> lights, Camera camera) {
		//shader.loadClipPlane(clipPlane);
		
		//need to be public variables in MasterRenderer
		shader.loadSkyColour(MasterRenderer.RED, MasterRenderer.GREEN, MasterRenderer.BLUE);
		
		org.saintandreas.math.Matrix4f viewMatrix = Maths.createViewMatrix(camera);
		
		shader.loadLights(lights);
		
		shader.loadViewMatrix(viewMatrix);
	}

}
