package stb.localiser.dynamic;
import java.io.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.util.*;
import java.nio.file.Files;
import java.util.stream.Collectors;

public class TestRunner {

    private static ArrayList<File> Recursive(File[] arr, ArrayList<File> files) {
         for(File f:arr) {
             if(f.isFile())
                  files.add(f);
             else if(f.isDirectory() && f.list().length>0)
                 Recursive(f.listFiles(), files);
         }
         return files;
    }

    public static void parse(String inputDir) throws Exception {
         File directory = null;
         FileInputStream fis = null;
         CharStream input = null;
         UUTLexer lexer = null;
         CommonTokenStream tokens = null;
         UUTParser parser = null;

         if(inputDir != null)
             directory = new File(inputDir);

         if(directory.exists() && directory.isDirectory()) {
              ArrayList<File> filesList = new ArrayList<>();
              File arr[] = directory.listFiles();
              filesList = Recursive(arr, new ArrayList<File>());

              if (!filesList.isEmpty()) {
                  
                //Loads pos and neg tests
                ArrayList<File> posTests =  filesList.stream()
                                              .filter(file -> file.toString().contains("pos"))
                                              .collect(Collectors.toCollection(ArrayList::new));
                ArrayList<File> negTests =  filesList.stream()
                                              .filter(file -> file.toString().contains("neg"))
                                              .collect(Collectors.toCollection(ArrayList::new));
                
                boolean failsPos = false;


                



                //Iterates over posTests, removes default eListenrs and attaches Spectra.getErrorListener
                //Then records num passes and fails and adds to logger
                   for(File file : posTests) {
                       if (file.getName().contains("UUT")) {
                            fis = new FileInputStream(file);
                            StringBuilder rawContent = new StringBuilder(Files.readString(file.toPath()));
                            while (rawContent.indexOf("/*") != -1) {
                                rawContent.delete(rawContent.indexOf("/*"), rawContent.indexOf("*/") + 2);
                            }
                            input = CharStreams.fromString(rawContent.toString());
                            // input = CharStreams.fromStream(fis);
                            lexer = new UUTLexer(input);
                            tokens = new CommonTokenStream(lexer);
                            parser = new UUTParser(tokens);
                            /* disable anltr error listeners */
                            parser.removeErrorListeners();
                            parser.addErrorListener(Spectra.getErrorListener());

                            ParseTree tree = parser.program();
                            ParseTreeWalker walker = new ParseTreeWalker();
                            ArrayList<String> spec = Spectra.produceSpectra(parser, tree, walker);
                            int noOfErrors = parser.getNumberOfSyntaxErrors();
                            boolean pos = file.getPath().contains("pos");
                            if ((noOfErrors == 0 && pos) || (noOfErrors != 0 && !pos)) {
                                Logger.trackPassed(file.getPath(), spec);
                            } else {
                                System.out.println("Failed test "  + input + " errCount: "+ noOfErrors);
                                Logger.trackFailed(file.getPath(), spec);
                            }
                       }
                   }
                   /**
                    * Not all the positive tests passed so we do not test the negative tests
                    */
                   if(Logger.getPassMap().size() != posTests.size()) {
                       Logger.spitJson();
                       System.exit(1);
                   } else {
                    for(File file : negTests) {
                        if (file.getName().contains("UUT")) {
                             fis = new FileInputStream(file);
                             input = CharStreams.fromStream(fis);
                             lexer = new UUTLexer(input);
                             tokens = new CommonTokenStream(lexer);
                             parser = new UUTParser(tokens);
                             /* disable anltr error listeners */
                             parser.removeErrorListeners();
                             parser.addErrorListener(Spectra.getErrorListener());
 
                             ParseTree tree = parser.program();
                             ParseTreeWalker walker = new ParseTreeWalker();
                             ArrayList<String> spec = Spectra.produceSpectra(parser, tree, walker);
                             int noOfErrors = parser.getNumberOfSyntaxErrors();
                             boolean pos = file.getPath().contains("pos");
                             if ((noOfErrors == 0 && pos) || (noOfErrors != 0 && !pos)) {
                                 Logger.trackPassed(file.getPath(), spec);
                             } else {
                                 System.out.println("Failed test "  + input + " errCount: "+ noOfErrors);
                                 Logger.trackFailed(file.getPath(), spec);
                             }
                        }
                    }
                    Logger.spitJson();
                    
                   }
                //    for(File file : filesList) {
                //        if (file.getName().contains("UUT")) {
                //             fis = new FileInputStream(file);
                //             input = CharStreams.fromStream(fis);
                //             lexer = new UUTLexer(input);
                //             tokens = new CommonTokenStream(lexer);
                //             parser = new UUTParser(tokens);
                //             /* disable anltr error listeners */
                //             parser.removeErrorListeners();
                //             parser.addErrorListener(Spectra.getErrorListener());

                //             ParseTree tree = parser.program();
                //             ParseTreeWalker walker = new ParseTreeWalker();
                //             ArrayList<String> spec = Spectra.produceSpectra(parser, tree, walker);
                //             int noOfErrors = parser.getNumberOfSyntaxErrors();
                //             boolean pos = file.getPath().contains("pos");
                //             if ((noOfErrors == 0 && pos) || (noOfErrors != 0 && !pos)) {
                //                 Logger.trackPassed(file.getPath(), spec);
                //             } else {
                //                 System.out.println("Failed test "  + input + " errCount: "+ noOfErrors);
                //                 Logger.trackFailed(file.getPath(), spec);
                            
                //             }
                //        }
                //    }
                   Logger.spitJson();
              }
         } else {
                System.out.println("Input is not a directory");
                System.exit(0);
         }
    }
}