package renderEngine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import models.Face;
import models.Group;
import models.RawModel;
import models.TexturedModel;
import objConverter.ModelData;
import objConverter.Vertex;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import textures.Material;

public class MyOBJLoader {

	static String prevMaterial = null;
	static int indicesMatCount = 0;
	static int previndicesMatCount = 0;

	public static TexturedModel loadOBJwithMtl(String fileName, Loader loader, boolean collidable){
		//Data management use
		
		final List<Vertex> vertices = new ArrayList<Vertex>();
		
		final List<Vector3f> verticesVector3 = new ArrayList<Vector3f>();
		final List<Vector2f> textures = new ArrayList<Vector2f>();
		final List<Vector3f> normals = new ArrayList<Vector3f>();
		final List<Integer> indices = new ArrayList<Integer>();
		
		//for mtl 
		//final List<Vector3f> colours = new ArrayList<Vector3f>();

		//list of nama material untuk model tu 
		final List<String> materials = new ArrayList<String>();

		//list nama material untuk sume indices
		final List<String> materialsOrder = new ArrayList<String>();
		
		//untuk nama material tu bape bnyk indices yang dia ada n perlu lukis 
		final HashMap<String, Integer[]> matIndicesCount = new HashMap<String, Integer[]>();
		
		//final List<Integer> IndicesToBeSorted = new ArrayList<Integer>();
		//final List<String> matToBeSorted = new ArrayList<String>();

		//Data needed for the VAO creation
		float[] verticesArray = null;
		float[] texturesArray = null;
		float[] normalsArray = null;
		int[] indicesArray = null;
		//float[] coloursArray = null;
		
		//int[] newIndicesArray = null;

		//to get all the materials name for the model
		String[] materialsNameArray = null;	
		String[] materialsOrderArray = null;
		
		int countMaterial = 0;

		//READ THE OBJ FILE
		FileReader fr = null;
		File file = new File("res/"+fileName+".obj");
		try {
			fr = new FileReader(file);
		} catch (FileNotFoundException e) {
			System.err.println("Couldn't load file!");
			e.printStackTrace();
		}
		BufferedReader reader = new BufferedReader(fr);
		String line;

		String currentMaterialName = null;
		TexturedModel m = new TexturedModel();

		int faceCount = 0;

		try {
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#")) {
					continue;
				}
				if (line.startsWith("mtllib ")) {
					//Get the material file name and open it 
					String materialFileName = line.split(" ")[1];
					File materialFile = new File(file.getParentFile().getAbsolutePath() + "/" + materialFileName);
					BufferedReader materialFileReader = new BufferedReader(new FileReader(materialFile));
					String materialLine;
					Material parseMaterial = new Material();
					String parseMaterialName = null;

					while ((materialLine = materialFileReader.readLine()) != null) {
						if (materialLine.startsWith("#")) {
							continue;
						}
						if (materialLine.startsWith("newmtl ")) {
							countMaterial++;
							//kalau nama tak sama dengan yg sebelum ni ... maksudnya data bhgn material tu da abes so 
							//boleh add ke hashmap dan teruskan baca material yang seterusnya 
							if(parseMaterialName != null){
								if (!parseMaterialName.equals(materialLine.split("")[1])) {
									//add the finished read material to the TexturedModel's HashMap
									materials.add(parseMaterialName);
									m.addMaterials(parseMaterialName, parseMaterial);							
								}
							}
							//tukar nama parseMaterial ke nama material yang baru 
							parseMaterialName = materialLine.split(" ")[1];
							parseMaterial = new Material(parseMaterialName);
						} else if (materialLine.startsWith("Ns ")) {
							parseMaterial.setShininess(Float.valueOf(materialLine.split(" ")[1]));
						} else if (materialLine.startsWith("Ka ")) {
							String[] rgb = materialLine.split(" ");
							
							//System.out.println("Here is rgb " + rgb[2]);
							
							parseMaterial.setAmbientColour(new Vector3f(Float.valueOf(rgb[1]), Float.valueOf(rgb[2]), Float.valueOf(rgb[3]))); 
						} else if (materialLine.startsWith("Ks ")) {
							String[] rgb = materialLine.split(" ");
							parseMaterial.setSpecularColour(new Vector3f(Float.valueOf(rgb[1]), Float.valueOf(rgb[2]), Float.valueOf(rgb[3]))); 
						} else if (materialLine.startsWith("Kd ")) {
							String[] rgb = materialLine.split(" ");
							parseMaterial.setDiffuseColour(new Vector3f(Float.valueOf(rgb[1]), Float.valueOf(rgb[2]), Float.valueOf(rgb[3])));  

						} else if (materialLine.startsWith("map_Kd")) {


							String check = materialLine.split(" ")[1];
							
							//System.out.println(check);
							
							String format = check.split(Pattern.quote("."))[1];

							parseMaterial.setTextureID(loader.loadTexture(file.getParentFile().getAbsolutePath() + "/" + materialLine.split(" ")[1].split(Pattern.quote("."))[0], format));


						} else if (materialLine.startsWith("Ni ")){

							parseMaterial.setIndexOfRefraction(Float.valueOf(materialLine.split(" ")[1]));

							/*Defines optical_density

							 Specifies the optical density for the surface.  This is also known as 
							index of refraction.

							 "optical_density" is the value for the optical density.  The values can 
							range from 0.001 to 10.  A value of 1.0 means that light does not bend 
							as it passes through an object.  Increasing the optical_density 
							increases the amount of bending.  Glass has an index of refraction of 
							about 1.5.  Values of less than 1.0 produce bizarre results and are not 
							recommended.*/
						} else if (materialLine.startsWith("d ")){
							//the transparency of the material
							parseMaterial.setTransparency(Float.valueOf(materialLine.split(" ")[1]));


						} else if (materialLine.startsWith("illum ")){
							//Define the illumination model: illum = 1 a flat material with no specular highlights
							//illum = 2 denotes the presence of specular highlights
							parseMaterial.setIllumination(Float.valueOf(materialLine.split(" ")[1]));

						} else {
							//I think this means space...
							//System.err.println("[MTL] Unknown Line: " + materialLine + " of file " + fileName);
						}	
					}

					//when finished reading the file we need to send the last material too
					materials.add(parseMaterialName);
					m.addMaterials(parseMaterialName, parseMaterial);							

					//System.out.println("Material count " + countMaterial);

					materialFileReader.close();

					//finished parsing .mtl file here next into the obj file 
				}  else if (line.startsWith("usemtl ")) {
					continue;
					//currentMaterialName = line.split(" ")[1];

				} else if (line.startsWith("v ")) {
					String[] currentLine = line.split(" ");
					
					Vector3f vertex = new Vector3f((float) Float.valueOf(currentLine[1]),
							(float) Float.valueOf(currentLine[2]),
							(float) Float.valueOf(currentLine[3]));
					
					Vertex newVertex = new Vertex(vertices.size(), vertex);
					
					vertices.add(newVertex);

					//kena tambah ke vertices list
					verticesVector3.add(vertex);

				} else if (line.startsWith("vn ")) {
					String[] xyz = line.split(" ");
					float x = Float.valueOf(xyz[1]);
					float y = Float.valueOf(xyz[2]);
					float z = Float.valueOf(xyz[3]);

					//kena tambah ke normals list
					normals.add(new Vector3f(x, y, z));

				} else if (line.startsWith("vt ")) {
					String[] xyz = line.split(" ");
					float s = Float.valueOf(xyz[1]);
					float t = Float.valueOf(xyz[2]);

					//kena tambah ke textures list
					textures.add(new Vector2f(s, t));

				} else if (line.startsWith("s ")) {
					//boolean enableSmoothShading = !line.contains("off");
					//m.setSmoothShadingEnabled(enableSmoothShading);
				} 
				else if (line.startsWith("f ")){
					faceCount++;

				}else if (line.startsWith("g ")){
					//do group thingy 
					continue;

				}else if (line.startsWith("o ")){
					//do object thingy
					continue;
				}else {
					System.err.println("[OBJ] Unknown Line: " + line + " of file " + fileName);
				}
			}
			
			//System.out.println("Face count " + faceCount);
			reader.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		texturesArray = new float[verticesVector3.size()*2] ;
		normalsArray = new float[verticesVector3.size()*3];

		//READ THE OBJ FILE AGAIN
		FileReader frf = null;
		File filef = new File("res/"+fileName+".obj");

		try {
			frf = new FileReader(filef);
		} catch (FileNotFoundException e) {
			System.err.println("Couldn't load file!");
			e.printStackTrace();
		}

		BufferedReader readerf = new BufferedReader(frf);
		String linef;

		try {
			
			while ((linef = readerf.readLine()) != null) {
				if (linef.startsWith("#")) {
					continue;
				}
				else if(linef.startsWith("usemtl ")){
					currentMaterialName = linef.split(" ")[1];
				}
				//yg start dengan selain f
				else if (!linef.startsWith("f ")) {
					continue;
				}
				else if (linef.startsWith("f ")) {
					//faceCount--;

					//make a face here 
					//Face f = new face();
					//faces.add(f);
					
					//add kat sini sbb ntuk satu face -> bukan satu indices 
					materialsOrder.add(currentMaterialName);
				

					//klu buat gini baru boleh dapat susunan sama 
					// indices kat situ  => material kat situ 			
					
					//tak perlu lagi ... 3 indices --> satu material 
					
					//IndicesToBeSorted.add(currentVertexPointer);
					//matToBeSorted.add(currentMaterial);
					
					if(prevMaterial == null){
						prevMaterial = currentMaterialName;
					}

					if(prevMaterial == currentMaterialName){
						//3 indices same ngan satu material
						indicesMatCount++;
					}
					else if(prevMaterial != currentMaterialName){
						//rekod untuk satu2 material tu bape kiraan faces dia

						Integer[] matCount = {previndicesMatCount, indicesMatCount}; 

						matIndicesCount.put(currentMaterialName, matCount);
						
						previndicesMatCount = indicesMatCount;
					}
					
					prevMaterial = currentMaterialName;


					String[] currentLine = linef.split(" ");

					String[] vertex1 = currentLine[1].split("/");
					String[] vertex2 = currentLine[2].split("/");
					String[] vertex3 = currentLine[3].split("/");

					//System.out.println("Contents " + Arrays.toString(vertex1));
					//System.out.println("Vertex1 " + vertex1.length);

					processVertexwithMtl(vertex1, indices, textures, normals, texturesArray, normalsArray);
					processVertexwithMtl(vertex2, indices, textures, normals, texturesArray, normalsArray);
					processVertexwithMtl(vertex3, indices, textures, normals, texturesArray, normalsArray);
				}
			}

			readerf.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		verticesArray = new float[verticesVector3.size()* 3] ;
		indicesArray = new int[indices.size()];
		
		//coloursArray = new float[verticesVector3.size() * 3];

		//hanya ada nama materials jek 
		materialsNameArray = new String[materials.size()];
		
		//ada susunan material ntuk sume face
		materialsOrderArray = new String[materialsOrder.size()];
		
		//dari materialsOrderArray ni kena 

		//ok no problem setakat ni 
		//System.out.println("Kiraan materials untuk setiap face (patut 222)" + materialsOrder.size());
		
		//populating the arrays
		//lepas da baca sume untuk v, vt and vn

		//dapatkan sume vertex 
		int vertexPointer = 0;
		for(Vector3f vertex:verticesVector3){
			verticesArray[vertexPointer++] = vertex.x;
			verticesArray[vertexPointer++] = vertex.y;
			verticesArray[vertexPointer++] = vertex.z;
		}
		
		float furthestPoint = 0;
		
		for (int i = 0; i < vertices.size(); i++) {
			Vertex currentVertex = vertices.get(i);
			if (currentVertex.getLength() > furthestPoint) {
				furthestPoint = currentVertex.getLength();
			}
			//kalau ada masalah tyme collision ntuk isaac bleh check sini 
			/*Vector3f position = currentVertex.getPosition();
			verticesArray[i * 3] = position.x;
			verticesArray[i * 3 + 1] = position.y;
			verticesArray[i * 3 + 2] = position.z;*/
			}

		for(int i = 0; i<indices.size(); i++){
			indicesArray[i] = indices.get(i);
		}

		for(int i = 0; i<materials.size(); i++){
			materialsNameArray[i] = materials.get(i);
		}
		
		for(int i = 0; i<materialsOrder.size(); i++){
			materialsOrderArray[i] = materialsOrder.get(i);
		}
		
		//dapatkan colour 
		
			//ni untuk setiap face kedo
			//satu face tu amek n letak 3 kali 
			/*for(int i = 0; i< materialsOrderArray.length; i++){
				//calculate colour from material 
				//tak guna texture yang ada gambar ... bleh ada colour jek... huhu 
				
				Material mat = TexturedModel.materialsHM.get(materialsOrderArray[i]);
				
				//kiraan 
				
				Vector3f col = new Vector3f(mat.getAmbientColour().getX() + mat.getDiffuseColour().getX() +  mat.getSpecularColour().getX(),
						mat.getAmbientColour().getY() + mat.getDiffuseColour().getY() +  mat.getSpecularColour().getY(),
						mat.getAmbientColour().getZ() + mat.getDiffuseColour().getZ() +  mat.getSpecularColour().getZ());
				
				//3 kali untuk 3 vertices td 
				colours.add(col);
				colours.add(col);
				colours.add(col);


				//mat.getShininess() 
				
				//mat.getIndexOfRefraction()

				//mat.getTransparency()

				//mat.getIllumination()
				
			}
			
			int colourPointer = 0;
			//cube try isi sume colour merah 
			for(Vector3f colour:colours){
				coloursArray[colourPointer++] = colour.x;
				coloursArray[colourPointer++] = colour.y;
				coloursArray[colourPointer++] = colour.z;
				
				coloursArray[colourPointer++] = 1.0f;
				coloursArray[colourPointer++] = 0.0f;
				coloursArray[colourPointer++] = 0.0f;
			}*/
			

		//System.out.println("MaterialsOrder (3 indices satu material) " + materialsOrder.size());
		
		//kalau nak laju rasanya kena load warna material ke ibo/vbo (kadai seterusnya)	
		//RawModel rm = loader.loadToVAO(verticesArray, textureArray, normalsArray, indicesArray, coloursArray);
		
		RawModel rm = loader.loadToVAO(verticesArray, texturesArray, normalsArray, indicesArray);

		if(collidable){
			rm.setCollidable(true);
		}
	/*	if(withData){
			ModelData data = new ModelData(verticesArray, texturesArray, 
					normalsArray, null, indicesArray, furthestPoint);
			rm.setModelData(data);
		}*/

		TexturedModel texModel = new TexturedModel(rm, true, fileName);	
		
		//nama material and bape bnyk material ada 
		texModel.setMaterialsNameArray(materialsNameArray);
		
		//ni klu nak kira dari indices mana ke mana guna material mana 
		texModel.setMaterialsOrderArray(materialsOrderArray);
		
		texModel.setMaterialCount(countMaterial);
		
		//ikut susunan material pertama bape bnyk indices yg ada 
		texModel.setMatIndicesCount(matIndicesCount);
		
		//System.out.println("End of parsing the Obj with Mtl file. Face Count = " + faceCount);

		return texModel;
	}

	private static void processVertexwithMtl(
			String[] vertexData, 
			List<Integer> indices, 
			List<Vector2f> textures, 
			List<Vector3f> normals, 
			float[] textureArray, 
			float[] normalsArray){

		if(vertexData.length == 3){

			//if there is no tex coords
			if(vertexData[1] == ""){
				System.out.println("masuk tak sini ");

			}
			//if the data is all complete 
			else{

				int currentVertexPointer = Math.abs(Integer.parseInt(vertexData[0])-1);

				indices.add(currentVertexPointer);

				if(!vertexData[1].equals("") && vertexData[1] != null){

					//System.out.println("vertexData[1] " + vertexData[1].toString() + " apa ye ");

					//kat sini dapatkan texture no brape dalam list yang dah ada

					Vector2f currentTex = textures.get(Math.abs(Integer.parseInt(vertexData[1]) - 1));

					textureArray[currentVertexPointer*2] = currentTex.x;
					textureArray[currentVertexPointer*2+1] = 1 - currentTex.y;
				}
				else{
					Vector2f currentTex = new Vector2f(0,0);
					textureArray[currentVertexPointer*2] = currentTex.x;
					textureArray[currentVertexPointer*2+1] = 1 - currentTex.y;
				}

				if(!vertexData[2].equals(" ") && vertexData[2] != null){

					Vector3f currentNorm = normals.get(Math.abs(Integer.parseInt(vertexData[2]) - 1));
					normalsArray[currentVertexPointer*3] = currentNorm.x;
					normalsArray[currentVertexPointer*3+1] = currentNorm.y;
					normalsArray[currentVertexPointer*3+2] = currentNorm.z;
				}
				else{
					Vector3f currentNorm = new Vector3f(0,0,0);
					normalsArray[currentVertexPointer*3] = currentNorm.x;
					normalsArray[currentVertexPointer*3+1] = currentNorm.y;
					normalsArray[currentVertexPointer*3+2] = currentNorm.z;
				}


			}

		}
		////in case there is no normals
		else if(vertexData.length < 3){

			int currentVertexPointer = Integer.parseInt(vertexData[0])-1;

			indices.add(currentVertexPointer);

			//System.out.println("No normals in OBJ model");

			if(!vertexData[1].equals(" ") && vertexData[1] != null){

				Vector2f currentTex = textures.get(Integer.parseInt(vertexData[1]) - 1);

				textureArray[currentVertexPointer*2] = currentTex.x;
				textureArray[currentVertexPointer*2+1] = 1 - currentTex.y;
			}
			else{
				//kat sini kasi kosong ek 
				Vector2f currentTex = new Vector2f(0,0);
				textureArray[currentVertexPointer*2] = currentTex.x;
				textureArray[currentVertexPointer*2+1] = 1 - currentTex.y;
			}
		}

	}

	//returns a RawModel and not textured, it is textured at the maingameloop
	/**
	 * 
	 * @param fileName ; the filename of the model 
	 * @param loader ; the loader 
	 * @param withData ; true if data is needed to be stored for calculations
	 * @return
	 */
	public static RawModel loadObjModel(String fileName, Loader loader, boolean collidable){

		List<Vertex> vertices = new ArrayList<Vertex>();
		List<Vector2f> textures = new ArrayList<Vector2f>();
		List<Vector3f> normals = new ArrayList<Vector3f>();
		List<Integer> indices = new ArrayList<Integer>();

		FileReader fr = null;
		try {
			fr = new FileReader(new File("res/"+fileName+".obj"));
		} catch (FileNotFoundException e) {
			System.err.println("Couldn't load file!");
			e.printStackTrace();
		}
		BufferedReader reader = new BufferedReader(fr);
		String line;

		try{
			//will read to the end of the file
			while((line = reader.readLine()) != null){
				String[] currentLine = line.split("\\s+");

				//vertices
				if(line.startsWith("v ")){
					Vector3f vertex = new Vector3f(
							Float.parseFloat(currentLine[1]), 
							Float.parseFloat(currentLine[2]), 
							Float.parseFloat(currentLine[3]));

					Vertex newVertex = new Vertex(vertices.size(), vertex);
					vertices.add(newVertex);		
				}
				//texture coordinate (only 2 numbers)
				else if(line.startsWith("vt ")){
					Vector2f texture  = new Vector2f(
							Float.parseFloat(currentLine[1]), 
							Float.parseFloat(currentLine[2]));
					textures.add(texture);
				}
				//normal vector
				else if(line.startsWith("vn ")){
					Vector3f normal = new Vector3f(
							Float.parseFloat(currentLine[1]), 
							Float.parseFloat(currentLine[2]), 
							Float.parseFloat(currentLine[3]));
					normals.add(normal);	
				}		
			}

			reader.close();

		}catch(Exception e){
			e.printStackTrace();
		}

		//parseFaces

		try {
			fr = new FileReader(new File("res/"+fileName+".obj"));
		} catch (FileNotFoundException e) {
			System.err.println("Couldn't load file!");
			e.printStackTrace();
		}

		BufferedReader readerF = new BufferedReader(fr);

		try{
			while((line = readerF.readLine()) != null){

				//ignores those lines not starting with f
				if(!line.startsWith("f ")){
					//line = reader.readLine();
					continue;
				}

				else if(line.startsWith("f ")){
					String[] currentLine = line.split(" ");

					String[] vertex1 = currentLine[1].split("/");
					String[] vertex2 = currentLine[2].split("/");
					String[] vertex3 = currentLine[3].split("/");

					Vertex v0 = processVertex(vertex1, vertices, indices);
					Vertex v1 = processVertex(vertex2, vertices, indices);
					Vertex v2 = processVertex(vertex3, vertices, indices);

					calculateTangents(v0, v1, v2, textures);
				}
			}
			readerF.close();
		}catch(Exception e){
			e.printStackTrace();
		}

		removeUnusedVertices(vertices);
		float[] verticesArray = new float[vertices.size() * 3];
		float[] texturesArray = new float[vertices.size() * 2];
		float[] normalsArray = new float[vertices.size() * 3];
		float[] tangentsArray = new float[vertices.size() * 3];
		float furthest = convertDataToArrays(vertices, textures, normals, verticesArray,
				texturesArray, normalsArray, tangentsArray);
		int[] indicesArray = convertIndicesListToArray(indices);

		RawModel rm = loader.loadToVAO(verticesArray, texturesArray, normalsArray, indicesArray);

		if(collidable){
			rm.setCollidable(true);
		}
		/*if(withData){
			ModelData data = new ModelData(verticesArray, texturesArray, 
					normalsArray, tangentsArray, indicesArray, furthest);
			rm.setModelData(data);
		}*/

		return rm;
	} 

	private static void calculateTangents(Vertex v0, Vertex v1, Vertex v2,
			List<Vector2f> textures) {
		Vector3f delatPos1 = Vector3f.sub(v1.getPosition(), v0.getPosition(), null);
		Vector3f delatPos2 = Vector3f.sub(v2.getPosition(), v0.getPosition(), null);

		//if there are textures
		if(textures.size() != 0){
			Vector2f uv0 = textures.get(v0.getTextureIndex());
			Vector2f uv1 = textures.get(v1.getTextureIndex());
			Vector2f uv2 = textures.get(v2.getTextureIndex());

			Vector2f deltaUv1 = Vector2f.sub(uv1, uv0, null);
			Vector2f deltaUv2 = Vector2f.sub(uv2, uv0, null);

			float r = 1.0f / (deltaUv1.x * deltaUv2.y - deltaUv1.y * deltaUv2.x);
			delatPos1.scale(deltaUv2.y);
			delatPos2.scale(deltaUv1.y);
			Vector3f tangent = Vector3f.sub(delatPos1, delatPos2, null);
			tangent.scale(r);

			v0.addTangent(tangent);
			v1.addTangent(tangent);
			v2.addTangent(tangent);
		}	
	}

	private static Vertex processVertex(String[] vertex, List<Vertex> vertices,
			List<Integer> indices) {

		int index = Integer.parseInt(vertex[0]) - 1;

		//to return it so need to define it here 
		//index dakara tak sepatutnya ada -1
		int textureIndex = -1;
		int normalIndex = -1;


		Vertex currentVertex = vertices.get(index);


		if(vertex.length == 3){
			//if texture is not available but normal is --> f 1/ /2 --> length should be 3 
			if(vertex[1].equals("")){
				//System.out.println("Only vertex and normal available");
				//masukkan 0 jek ntuk texture ni ok? 

				//textureIndex = 0;

				normalIndex = Integer.parseInt(vertex[2]) - 1;
			}

			//if all data is available 
			else if(!vertex[1].equals("") && vertex[1] != null && !vertex[2].equals("") && vertex[2] != null){
				textureIndex = Integer.parseInt(vertex[1]) - 1;

				normalIndex = Integer.parseInt(vertex[2]) - 1;
			}	

		}
		//if only vertex n texture are present 
		else if(vertex.length == 2){ //f 2/1 --> normal tkde 
			//if texture is not empty and not null 

			if(!vertex[1].equals(" ") && vertex[1] != null){
				textureIndex = Integer.parseInt(vertex[1]) - 1;

				//System.out.println("Normals not available");
			}	
		}
		//if only vertex is present 
		else if(vertex.length == 1){
			System.out.println("Ada ke yang takde texture n normal langsung?");
		}

		if (!currentVertex.isSet()) {
			currentVertex.setTextureIndex(textureIndex);
			currentVertex.setNormalIndex(normalIndex);
			indices.add(index);
			return currentVertex;
		} else {
			return dealWithAlreadyProcessedVertex(currentVertex, textureIndex, normalIndex, indices,
					vertices);
		}	
	}

	private static int[] convertIndicesListToArray(List<Integer> indices) {
		int[] indicesArray = new int[indices.size()];
		for (int i = 0; i < indicesArray.length; i++) {
			indicesArray[i] = indices.get(i);
		}
		return indicesArray;
	}

	private static float convertDataToArrays(List<Vertex> vertices, List<Vector2f> textures,
			List<Vector3f> normals, float[] verticesArray, float[] texturesArray,
			float[] normalsArray, float[] tangentsArray) {
		float furthestPoint = 0;
		for (int i = 0; i < vertices.size(); i++) {
			Vertex currentVertex = vertices.get(i);
			if (currentVertex.getLength() > furthestPoint) {
				furthestPoint = currentVertex.getLength();
			}
			Vector3f position = currentVertex.getPosition();
			verticesArray[i * 3] = position.x;
			verticesArray[i * 3 + 1] = position.y;
			verticesArray[i * 3 + 2] = position.z;

			//System.out.println(currentVertex.getTextureIndex());

			if(textures != null){
				if(textures.size() != 0){
					Vector2f textureCoord = textures.get(currentVertex.getTextureIndex());

					texturesArray[i * 2] = textureCoord.x;
					texturesArray[i * 2 + 1] = 1 - textureCoord.y;
				}
				//if there are no textures we need to set them -> 0 
				else{
					Vector2f textureCoord = new Vector2f(0,0);

					texturesArray[i * 2] = textureCoord.x;
					texturesArray[i * 2 + 1] = 1 - textureCoord.y;
				}
			}

			if(normals != null){
				if(normals.size() != 0){
					Vector3f normalVector = normals.get(currentVertex.getNormalIndex());

					normalsArray[i * 3] = normalVector.x;
					normalsArray[i * 3 + 1] = normalVector.y;
					normalsArray[i * 3 + 2] = normalVector.z;
				}
				//if there are no normals we need to set them -> 0 
				else{
					Vector3f normalVector = new Vector3f(0,0,0);

					normalsArray[i * 3] = normalVector.x;
					normalsArray[i * 3 + 1] = normalVector.y;
					normalsArray[i * 3 + 2] = normalVector.z;

				}
			}

			Vector3f tangent = currentVertex.getAverageTangent();
			tangentsArray[i * 3] = tangent.x;
			tangentsArray[i * 3 + 1] = tangent.y;
			tangentsArray[i * 3 + 2] = tangent.z;

		}
		return furthestPoint;
	}

	/*private static void processVertex(
			String[] vertexData, 
			List<Integer> indices, 
			List<Vector2f> textures, 
			List<Vector3f> normals, 
			float[] textureArray, 
			float[] normalsArray){

		// string[] vertexData --> [0] -> vertex, [1] -> texture, [2] -> normals
		// ni tolak 1 sbb dr blender datte

		if(vertexData.length == 3){

			int currentVertexPointer = Integer.parseInt(vertexData[0])-1;
			indices.add(currentVertexPointer);

			if(!vertexData[1].equals(" ") && vertexData[1] != null){

				//System.out.println("vertexData[1]" + Integer.parseInt(vertexData[1]));

				Vector2f currentTex = textures.get(Integer.parseInt(vertexData[1]) - 1);

				textureArray[currentVertexPointer*2] = currentTex.x;
				textureArray[currentVertexPointer*2+1] = 1 - currentTex.y;
			}
			else{
				Vector2f currentTex = new Vector2f(0,0);
				textureArray[currentVertexPointer*2] = currentTex.x;
				textureArray[currentVertexPointer*2+1] = 1 - currentTex.y;
			}

			if(!vertexData[2].equals(" ") && vertexData[2] != null){

				Vector3f currentNorm = normals.get(Integer.parseInt(vertexData[2]) - 1);
				normalsArray[currentVertexPointer*3] = currentNorm.x;
				normalsArray[currentVertexPointer*3+1] = currentNorm.y;
				normalsArray[currentVertexPointer*3+2] = currentNorm.z;
			}
			else{
				Vector3f currentNorm = new Vector3f(0,0,0);
				normalsArray[currentVertexPointer*3] = currentNorm.x;
				normalsArray[currentVertexPointer*3+1] = currentNorm.y;
				normalsArray[currentVertexPointer*3+2] = currentNorm.z;
			}


		}
		////in case there is no normals
		else if(vertexData.length < 3){
			int currentVertexPointer = Integer.parseInt(vertexData[0])-1;
			indices.add(currentVertexPointer);

			if(!vertexData[1].equals(" ") && vertexData[1] != null){

				Vector2f currentTex = textures.get(Integer.parseInt(vertexData[1]) - 1);

				textureArray[currentVertexPointer*2] = currentTex.x;
				textureArray[currentVertexPointer*2+1] = 1 - currentTex.y;
			}
			else{
				Vector2f currentTex = new Vector2f(0,0);
				textureArray[currentVertexPointer*2] = currentTex.x;
				textureArray[currentVertexPointer*2+1] = 1 - currentTex.y;
			}
		}
	}*/

	private static Vertex dealWithAlreadyProcessedVertex(Vertex previousVertex, int newTextureIndex,
			int newNormalIndex, List<Integer> indices, List<Vertex> vertices) {
		if (previousVertex.hasSameTextureAndNormal(newTextureIndex, newNormalIndex)) {
			indices.add(previousVertex.getIndex());
			return previousVertex;
		} else {
			Vertex anotherVertex = previousVertex.getDuplicateVertex();
			if (anotherVertex != null) {
				return dealWithAlreadyProcessedVertex(anotherVertex, newTextureIndex,
						newNormalIndex, indices, vertices);
			} else {
				Vertex duplicateVertex = new Vertex(vertices.size(), previousVertex.getPosition());
				duplicateVertex.setTextureIndex(newTextureIndex);
				duplicateVertex.setNormalIndex(newNormalIndex);
				previousVertex.setDuplicateVertex(duplicateVertex);
				vertices.add(duplicateVertex);
				indices.add(duplicateVertex.getIndex());
				return duplicateVertex;
			}

		}
	}

	private static void removeUnusedVertices(List<Vertex> vertices) {
		for (Vertex vertex : vertices) {
			vertex.averageTangents();
			if (!vertex.isSet()) {
				vertex.setTextureIndex(0);
				vertex.setNormalIndex(0);
			}
		}
	}
}
